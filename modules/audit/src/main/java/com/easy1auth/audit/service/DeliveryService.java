package com.easy1auth.audit.service;

import com.easy1auth.audit.constant.ErrorCodeConstants;
import com.easy1auth.audit.dto.AuditSubscriptionInput;
import com.easy1auth.audit.dto.AuditSubscriptionView;
import com.easy1auth.audit.model.*;
import com.easy1auth.audit.repository.DeliveryRepository;
import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.id.UuidV7;
import com.easy1auth.security.SecurityDataCipher;
import com.easy1auth.common.foundation.util.TenantContextHolder;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 投递服务：Webhook 订阅管理与审计事件投递（outbox 模式）。
 *
 * <p>负责 Webhook 订阅的增删改查、密钥生成与轮换，以及将审计事件/邮件写入 delivery_outbox 待投递队列。队列由外部调度器通过 claim / sent / failed
 * 推进状态机（pending → processing → sent / dead），processing 带租约防重复。
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

  /** 将审计事件写入所有匹配订阅的 Webhook 投递队列（含幂等键防止重复投递）。 */
  @Transactional
  public void enqueueEvent(UUID tenant, String event, Map<String, Object> payload, String key) {
    for (var hook : repository.findActiveSubscriptions(tenant)) {
      if (hook.events().contains(event) || hook.events().contains("*")) {
        enqueue(
            tenant,
            "webhook",
            hook.url(),
            event,
            payload,
            hook.id(),
            key + ":" + hook.id(),
            hook.maxRetries());
      }
    }
  }

  /** 将邮件投递任务写入投递队列（默认最大重试 5 次）。 */
  @Transactional
  public void enqueueEmail(UUID tenant, String to, String subject, String body, String key) {
    enqueue(tenant, "email", to, "email", Map.of("subject", subject, "body", body), null, key, 5);
  }

  /** 领取一批到期可投递的 outbox 记录并置为 processing（带 60 秒租约，限最多 50 条）。 */
  @Transactional
  public List<DeliveryOutboxEntity> claim(int limit) {
    Instant now = Instant.now();
    return repository.claim(limit, now);
  }

  /** 标记投递记录发送成功（记录发送时间并释放租约）。 */
  @Transactional
  public void sent(UUID id) {
    repository.markSent(id, Instant.now());
  }

  /** 标记投递失败：按指数退避（上限 1 小时）重排待投递，超过最大次数则置为 dead。 */
  @Transactional
  public void failed(UUID id, String error) {
    var row = repository.findOutboxForUpdate(id);
    int n = row.attempts() + 1;
    boolean dead = n >= row.maxAttempts();
    long delay = Math.min(3600L, 5L * (1L << Math.min(10, n)));
    repository.markFailed(
        id,
        dead ? "dead" : "pending",
        n,
        Instant.now().plusSeconds(delay + random.nextInt(5)),
        trim(error, 1000));
  }

  /** 解密并返回投递记录对应 Webhook 订阅的密钥（投递签名校验用）。 */
  @Transactional(readOnly = true)
  public String webhookSecret(DeliveryOutboxEntity row) {
    var hook = entity(row.tenantId(), row.subscriptionId());
    return cipher.decrypt("webhook:" + hook.tenantId() + ":" + hook.id(), hook.encryptedSecret());
  }

  /** 写入一条投递记录（INSERT_IF_ABSENT，按幂等键去重）。 */
  private void enqueue(
      UUID tenant,
      String channel,
      String dest,
      String event,
      Map<String, Object> payload,
      UUID subscription,
      String key,
      int retries) {
    Instant now = Instant.now();
    var e =
        DeliveryOutboxEntityDraft.$.produce(
            d ->
                d.setId(UuidV7.randomUuid())
                    .setTenantId(tenant)
                    .setChannel(channel)
                    .setDestination(dest)
                    .setEventType(event)
                    .setPayload(payload)
                    .setSubscriptionId(subscription)
                    .setIdempotencyKey(key)
                    .setStatus("pending")
                    .setAttempts(0)
                    .setMaxAttempts(retries)
                    .setAvailableAt(now)
                    .setLeaseUntil(null)
                    .setLastError(null)
                    .setCreatedAt(now)
                    .setSentAt(null));
    repository.saveOutbox(e);
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

  /** 将字符串截断到指定长度（null 原样返回）。 */
  private static String trim(String v, int n) {
    return v == null ? null : v.substring(0, Math.min(n, v.length()));
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
