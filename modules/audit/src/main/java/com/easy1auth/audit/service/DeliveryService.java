package com.easy1auth.audit.service;

import com.easy1auth.audit.constant.ErrorCodeConstants;
import com.easy1auth.audit.dto.AuditSubscriptionInput;
import com.easy1auth.audit.dto.AuditSubscriptionView;
import com.easy1auth.audit.model.WebhookSubscriptionEntity;
import com.easy1auth.audit.model.WebhookSubscriptionEntityDraft;
import com.easy1auth.audit.repository.DeliveryRepository;
import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.id.UuidV7;
import com.easy1auth.security.SecurityDataCipher;
import com.easy1auth.common.foundation.util.TenantContextHolder;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Webhook 订阅服务。
 *
 * <p>负责 Webhook 订阅的增删改查、密钥生成与轮换。具体 Webhook 消息由 Redis Stream
 * 消费者处理，不在本模块维护投递队列。</p>
 */
@Service
public class DeliveryService {
  /** jimmer SQL 客户端 */
  private final DeliveryRepository repository;

  /** 数据加解密器（用于加密存储 Webhook 密钥） */
  private final SecurityDataCipher cipher;

  /** 安全随机数生成器（用于生成密钥与投递退避抖动） */
  private final SecureRandom random = new SecureRandom();

  public DeliveryService(DeliveryRepository repository, SecurityDataCipher cipher) {
    this.repository = repository;
    this.cipher = cipher;
  }

  /** 创建 Webhook 订阅，生成并加密存储订阅密钥，返回含密钥的订阅视图。 */
  @Transactional
  public AuditSubscriptionView create(AuditSubscriptionInput in) {
    UUID tenant = TenantContextHolder.requireTenantId();
    validate(in);
    UUID id = UuidV7.randomUuid();
    String secret = token(32);
    Instant now = Instant.now();
    var e =
        WebhookSubscriptionEntityDraft.$.produce(
            d ->
                d.setId(id)
                    .setName(in.name().strip())
                    .setUrl(in.url())
                    .setEvents(in.events().stream().distinct().toList())
                    .setSecretHash(hash(secret))
                    .setEncryptedSecret(cipher.encrypt("webhook:" + tenant + ":" + id, secret))
                    .setStatus("active")
                    .setMaxRetries(in.maxRetries() == null ? 5 : in.maxRetries())
                    .setCreatedAt(now)
                    .setUpdatedAt(now));
    repository.saveSubscription(e);
    return view(e, secret);
  }

  /** 查询当前租户全部 Webhook 订阅（按创建时间倒序，不返回密钥）。 */
  @Transactional(readOnly = true)
  public List<AuditSubscriptionView> list() {
    return repository.listSubscriptions().stream().map(e -> view(e, null)).toList();
  }

  /** 更新 Webhook 订阅信息（名称/地址/事件/重试次数/状态），仅更新传入的非空字段。 */
  @Transactional
  public AuditSubscriptionView update(UUID id, AuditSubscriptionInput in) {
    var old = entity(id);
    validate(in);
    repository.updateSubscription(
        old.tenantId(),
        id,
        in.name(),
        in.url(),
        in.events(),
        in.maxRetries() == null ? old.maxRetries() : in.maxRetries(),
        in.status() == null ? old.status() : status(in.status()));
    return view(entity(id), null);
  }

  /** 轮换 Webhook 订阅密钥并返回新密钥（旧密钥失效）。 */
  @Transactional
  public AuditSubscriptionView rotate(UUID id) {
    var old = entity(id);
    String secret = token(32);
    repository.rotateSubscriptionSecret(
        old.tenantId(),
        id,
        hash(secret),
        cipher.encrypt("webhook:" + old.tenantId() + ":" + id, secret));
    return view(entity(id), secret);
  }

  /** 删除 Webhook 订阅（限当前租户）。 */
  @Transactional
  public void delete(UUID id) {
    var old = entity(id);
    if (repository.deleteSubscription(old.tenantId(), id) != 1) {
      throw missing();
    }
  }

  /** 按租户与 ID 查询订阅实体，不存在时抛出领域异常。 */
  private WebhookSubscriptionEntity entity(UUID tenant, UUID id) {
    return repository.findSubscription(tenant, id).orElseThrow(this::missing);
  }

  /** 按 ID 查询订阅实体（不限租户），不存在时抛出领域异常。 */
  private WebhookSubscriptionEntity entity(UUID id) {
    return repository.findSubscription(id).orElseThrow(this::missing);
  }

  /** 构造订阅缺失的领域异常。 */
  private DomainException missing() {
    return new DomainException(ErrorCodeConstants.WEBHOOK_NOT_FOUND);
  }

  /** 校验订阅输入：名称、事件非空，地址须为公网 HTTPS，重试次数在 0-20 之间。 */
  private static void validate(AuditSubscriptionInput in) {
    if (in == null
        || in.name() == null
        || in.name().isBlank()
        || in.events() == null
        || in.events().isEmpty()) {
      throw new DomainException(ErrorCodeConstants.WEBHOOK_INVALID);
    }
    try {
      URI u = URI.create(in.url());
      if (!"https".equals(u.getScheme()) || u.getHost() == null) {
        throw new IllegalArgumentException();
      }
      for (var a : java.net.InetAddress.getAllByName(u.getHost())) {
        if (a.isAnyLocalAddress()
            || a.isLoopbackAddress()
            || a.isLinkLocalAddress()
            || a.isSiteLocalAddress()) {
          throw new IllegalArgumentException();
        }
      }
    } catch (Exception ex) {
      throw new DomainException(ErrorCodeConstants.WEBHOOK_URL_FORBIDDEN);
    }
    if (in.maxRetries() != null && (in.maxRetries() < 0 || in.maxRetries() > 20)) {
      throw new DomainException(ErrorCodeConstants.WEBHOOK_RETRY_INVALID);
    }
  }

  /** 生成 Base64 URL 安全的随机令牌（用于 Webhook 密钥）。 */
  private String token(int n) {
    byte[] b = new byte[n];
    random.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  /** 计算输入字符串的 SHA-256 十六进制摘要。 */
  private static String hash(String v) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /** 校验并规范化订阅状态（仅允许 active / disabled）。 */
  private static String status(String v) {
    if (!Set.of("active", "disabled").contains(v)) {
      throw new DomainException(ErrorCodeConstants.WEBHOOK_STATUS_INVALID);
    }
    return v;
  }

  /** 组装订阅视图（secret 仅在创建/轮换后非空）。 */
  private static AuditSubscriptionView view(WebhookSubscriptionEntity e, String secret) {
    return new AuditSubscriptionView(
        e.id(),
        e.tenantId(),
        e.name(),
        e.url(),
        e.events(),
        secret,
        e.status(),
        e.maxRetries(),
        e.createdAt(),
        e.updatedAt());
  }
}
