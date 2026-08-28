package com.easy1auth.social.service;

import com.easy1auth.poolidentity.service.PoolUserService;
import com.easy1auth.poolidentity.service.PoolUserInput;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.id.UuidV7;
import com.easy1auth.infrastructure.foundation.web.PageData;
import com.easy1auth.security.SecurityDataCipher;
import com.easy1auth.social.ErrorCodeConstants;
import com.easy1auth.social.adapter.SocialIdentityAdapter;
import com.easy1auth.social.model.*;
import com.easy1auth.tenant.TenantContextHolder;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * 社会化身份源服务：管理身份源 CRUD，并编排社交登录 authorize/callback 流程。
 *
 * <p>厂商协议（授权 URL、token 交换、用户信息拉取）委托给 {@link SocialIdentityAdapter}，
 * 厂商无关逻辑（state/nonce 生成、事务存取、账户确认后的绑定）在本类内统一处理。</p>
 */
@Service
public class SocialIdentityService {

    /** social_identity_source 表静态描述符 */
    private static final SocialIdentitySourceEntityTable SOURCE = SocialIdentitySourceEntityTable.$;
    /** social_login_transaction 表静态描述符 */
    private static final SocialLoginTransactionEntityTable TX = SocialLoginTransactionEntityTable.$;
    /** social_identity_binding 表静态描述符 */
    private static final SocialIdentityBindingEntityTable BINDING = SocialIdentityBindingEntityTable.$;

    /** jimmer SQL 客户端 */
    private final JSqlClient sql;
    /** 数据加密器（用于加密 client_secret / nonce / PKCE verifier） */
    private final SecurityDataCipher cipher;
    /** 用户服务（JIT 开通时创建 pool_user） */
    private final PoolUserService users;
    /** 随机源（生成 state / nonce / PKCE 参数） */
    private final SecureRandom random = new SecureRandom();
    /** 按厂商类型索引的适配器映射 */
    private final Map<String, SocialIdentityAdapter> adapters;

    public SocialIdentityService(JSqlClient sql, SecurityDataCipher cipher, PoolUserService users, List<SocialIdentityAdapter> adapterList) {
        this.sql = sql;
        this.cipher = cipher;
        this.users = users;
        Map<String, SocialIdentityAdapter> map = new LinkedHashMap<>();
        for (var a : adapterList) {
            map.put(a.type(), a);
        }
        this.adapters = Map.copyOf(map);
    }

    // ==================== 管理 API ====================

    /** 创建身份源：校验参数后加密保存 client_secret，初始状态为 active。 */
    @Transactional
    public SourceView create(UUID tenant, SocialIdentityInput in) {
        validate(in, true);
        UUID id = UuidV7.randomUuid();
        Instant now = Instant.now();
        var e = SocialIdentitySourceEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant)
                .setName(in.name().strip())
                .setType(in.type())
                .setMode(in.mode())
                .setClientId(in.clientId().strip())
                .setEncryptedClientSecret(cipher.encrypt("social:" + tenant + ":" + id, in.clientSecret()))
                .setJitProvisioning(Boolean.TRUE.equals(in.jitProvisioning()))
                .setStatus("active").setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return view(e, in.clientSecret());
    }

    /** 分页查询租户下的身份源，支持按名称模糊搜索与状态过滤。 */
    @Transactional(readOnly = true)
    public PageData<SourceView> list(UUID tenant, int page, int size, String search, String status) {
        int p = Math.max(1, page), s = Math.min(100, Math.max(1, size));
        var q = sql.createQuery(SOURCE).where(SOURCE.tenantId().eq(tenant))
                .whereIf(search != null && !search.isBlank(), () -> SOURCE.name().ilike(search, LikeMode.ANYWHERE))
                .whereIf(status != null && !status.isBlank(), () -> SOURCE.status().eq(status))
                .orderBy(SOURCE.createdAt().desc()).select(SOURCE);
        long total = q.fetchUnlimitedCount();
        return PageData.of(q.limit(s, (long) (p - 1) * s).execute().stream().map(e -> view(e, null)).toList(), p, s, total);
    }

    /** 查询单个身份源详情（不返回明文 client_secret）。 */
    @Transactional(readOnly = true)
    public SourceView get(UUID tenant, UUID id) {
        return view(entity(tenant, id), null);
    }

    /** 更新身份源：仅更新传入的非空字段，client_secret 非空时重新加密保存。 */
    @Transactional
    public SourceView update(UUID tenant, UUID id, SocialIdentityInput in) {
        var old = entity(tenant, id);
        validate(in, false);
        var u = sql.createUpdate(SOURCE).set(SOURCE.updatedAt(), Instant.now())
                .where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id));
        if (in.name() != null) {
            u.set(SOURCE.name(), in.name().strip());
        }
        if (in.type() != null) {
            u.set(SOURCE.type(), in.type());
        }
        if (in.mode() != null) {
            u.set(SOURCE.mode(), in.mode());
        }
        if (in.clientId() != null) {
            u.set(SOURCE.clientId(), in.clientId().strip());
        }
        if (in.clientSecret() != null && !in.clientSecret().isBlank()) {
            u.set(SOURCE.encryptedClientSecret(), cipher.encrypt("social:" + tenant + ":" + id, in.clientSecret()));
        }
        if (in.jitProvisioning() != null) {
            u.set(SOURCE.jitProvisioning(), in.jitProvisioning());
        }
        if (in.status() != null) {
            u.set(SOURCE.status(), status(in.status()));
        }
        u.execute();
        return get(tenant, id);
    }

    /** 删除身份源（物理删除，须属于指定租户）。 */
    @Transactional
    public void delete(UUID tenant, UUID id) {
        if (sql.createDelete(SOURCE).where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id)).execute() != 1) {
            throw missing();
        }
    }

    /** 查询租户下已启用的身份源（供登录页渲染按钮用）。 */
    @Transactional(readOnly = true)
    public List<SourceView> listActive(UUID tenant) {
        return sql.createQuery(SOURCE).where(SOURCE.tenantId().eq(tenant), SOURCE.status().eq("active"))
                .orderBy(SOURCE.createdAt().asc()).select(SOURCE).execute().stream()
                .map(e -> view(e, null)).toList();
    }

    // ==================== 登录流程 ====================

    /** 发起社交登录：校验身份源后生成 state/nonce/PKCE，落库事务并返回跳转授权 URL。 */
    @Transactional
    public AuthorizationStart authorize(UUID tenant, UUID sourceId, String redirectUri) {
        var s = entity(tenant, sourceId);
        if (!"active".equals(s.status())) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_SOURCE_DISABLED);
        }
        var adapter = adapter(s.type());
        URI callback = safeRedirect(redirectUri);
        String state = token(32), nonce = token(32);
        String verifier = adapter.supportsPkce() ? token(48) : null;
        String challenge = verifier != null ? base64(sha256(verifier)) : null;
        UUID id = UuidV7.randomUuid();
        Instant now = Instant.now();
        var tx = SocialLoginTransactionEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant)
                .setSourceId(sourceId)
                .setStateHash(hash(state)).setNonceHash(hash(nonce))
                .setEncryptedNonce(cipher.encrypt("social-tx:" + id + ":nonce", nonce))
                .setEncryptedPkceVerifier(verifier != null ? cipher.encrypt("social-tx:" + id + ":pkce", verifier) : null)
                .setReturnUri(callback.toString()).setExpiresAt(now.plusSeconds(600))
                .setConsumedAt(null).setCreatedAt(now));
        sql.saveCommand(tx).setMode(SaveMode.INSERT_ONLY).execute();
        var result = adapter.authorize(s.clientId(), callback, state, adapter.defaultScope(), challenge);
        return new AuthorizationStart(result.authorizeUrl().toString(), state, 600);
    }

    /** 处理回调：校验事务并拉取用户信息，若已有绑定则返回该用户并刷新最后登录时间。 */
    @Transactional
    public CallbackResult callback(String code, String state, String redirectUri) {
        var tx = sql.createQuery(TX).where(TX.stateHash().eq(hash(state)))
                .select(TX).forUpdate().fetchOneOrNull();
        if (tx == null || tx.consumedAt() != null || tx.expiresAt().isBefore(Instant.now()) || !Objects.equals(tx.returnUri(), redirectUri)) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TRANSACTION_INVALID);
        }
        UUID tenant = tx.tenantId();
        UUID sourceId = tx.sourceId();
        var s = entity(tenant, sourceId);
        var adapter = adapter(s.type());
        String secret = cipher.decrypt("social:" + tenant + ":" + sourceId, s.encryptedClientSecret());
        String verifier = tx.encryptedPkceVerifier() != null
                ? cipher.decrypt("social-tx:" + tx.id() + ":pkce", tx.encryptedPkceVerifier()) : null;
        var info = adapter.exchangeAndFetch(s.clientId(), secret, code, safeRedirect(redirectUri), verifier);

        var binding = sql.createQuery(BINDING)
                .where(BINDING.tenantId().eq(tenant), BINDING.sourceId().eq(sourceId), BINDING.subject().eq(info.subject()))
                .select(BINDING).fetchOneOrNull();
        UUID userId = null;
        if (binding != null) {
            userId = binding.poolUserId();
            sql.createUpdate(BINDING).set(BINDING.lastLoginAt(), Instant.now()).where(BINDING.id().eq(binding.id())).execute();
        }
        sql.createUpdate(TX).set(TX.consumedAt(), Instant.now()).where(TX.id().eq(tx.id()), TX.consumedAt().isNull()).execute();
        var identity = new PendingIdentity(tenant, sourceId, s.type(), info.subject(), info.username(),
                info.name(), info.email(), info.avatar());
        return new CallbackResult(tenant, sourceId, userId, identity);
    }

    /** 用户明确确认后，创建一个新的 pool_user 并绑定已验证的社会化身份。 */
    @Transactional
    public UUID provision(PendingIdentity identity, String username) {
        if (identity == null || blank(identity.email())) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_EMAIL_REQUIRED);
        }
        String name = blank(identity.name()) ? identity.email() : identity.name();
        var user = users.create(identity.tenantId(), new PoolUserInput(username, identity.email(), null, null,
                name, identity.avatar(), null, null, null, Map.of("federated", true, "social_type", identity.sourceType())));
        bind(identity, user.id());
        return user.id();
    }

    /** 将已验证的社会化身份绑定到已通过本地认证的 pool_user。 */
    @Transactional
    public void bind(PendingIdentity identity, UUID poolUserId) {
        if (identity == null || poolUserId == null) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TRANSACTION_INVALID);
        }
        var existing = sql.createQuery(BINDING)
                .where(BINDING.tenantId().eq(identity.tenantId()), BINDING.sourceId().eq(identity.sourceId()), BINDING.subject().eq(identity.subject()))
                .select(BINDING).fetchOneOrNull();
        if (existing != null) {
            if (!existing.poolUserId().equals(poolUserId)) {
                throw new DomainException(ErrorCodeConstants.SOCIAL_BINDING_CONFLICT);
            }
            sql.createUpdate(BINDING).set(BINDING.lastLoginAt(), Instant.now()).where(BINDING.id().eq(existing.id())).execute();
            return;
        }
        var binding = SocialIdentityBindingEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(identity.tenantId())
                .setSourceId(identity.sourceId()).setPoolUserId(poolUserId).setSourceType(identity.sourceType()).setSubject(identity.subject())
                .setClaims(Map.of("name", Objects.toString(identity.name(), ""), "email", Objects.toString(identity.email(), ""),
                        "username", Objects.toString(identity.username(), "")))
                .setCreatedAt(Instant.now()).setLastLoginAt(Instant.now()));
        sql.saveCommand(binding).setMode(SaveMode.INSERT_ONLY).execute();
    }

    // ==================== TenantContextHolder 便捷重载 ====================

    /** 从租户上下文创建身份源。 */
    @Transactional
    public SourceView create(SocialIdentityInput in) { return create(TenantContextHolder.requireTenantId(), in); }

    /** 从租户上下文分页查询身份源。 */
    @Transactional(readOnly = true)
    public PageData<SourceView> list(int page, int size, String search, String status) {
        return list(TenantContextHolder.requireTenantId(), page, size, search, status);
    }

    /** 从租户上下文查询单个身份源。 */
    @Transactional(readOnly = true)
    public SourceView get(UUID id) { return get(TenantContextHolder.requireTenantId(), id); }

    /** 从租户上下文更新身份源。 */
    @Transactional
    public SourceView update(UUID id, SocialIdentityInput in) { return update(TenantContextHolder.requireTenantId(), id, in); }

    /** 从租户上下文删除身份源。 */
    @Transactional
    public void delete(UUID id) { delete(TenantContextHolder.requireTenantId(), id); }

    /** 从租户上下文查询已启用身份源。 */
    @Transactional(readOnly = true)
    public List<SourceView> listActive() { return listActive(TenantContextHolder.requireTenantId()); }

    // ==================== 私有辅助 ====================

    /** 按厂商类型获取适配器，不支持的类型抛出异常。 */
    private SocialIdentityAdapter adapter(String type) {
        var a = adapters.get(type);
        if (a == null) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TYPE_NOT_SUPPORTED);
        }
        return a;
    }

    /** 查询租户下指定身份源，不存在时抛出异常。 */
    private SocialIdentitySourceEntity entity(UUID tenant, UUID id) {
        return sql.createQuery(SOURCE).where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id)).select(SOURCE)
                .fetchOptional().orElseThrow(this::missing);
    }

    private DomainException missing() { return new DomainException(ErrorCodeConstants.SOCIAL_SOURCE_NOT_FOUND); }

    /** 校验身份源入参：创建时必须齐全，类型须受支持，状态合法。 */
    private void validate(SocialIdentityInput i, boolean create) {
        if (i == null || (create && (blank(i.name()) || blank(i.type()) || blank(i.clientId()) || blank(i.clientSecret())))) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_SOURCE_INVALID);
        }
        if (i.type() != null && !adapters.containsKey(i.type())) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_TYPE_NOT_SUPPORTED);
        }
        if (i.status() != null) {
            status(i.status());
        }
    }

    /** 校验并返回合法状态（active / disabled）。 */
    private static String status(String s) {
        if (!Set.of("active", "disabled").contains(s)) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_STATUS_INVALID);
        }
        return s;
    }

    /** 将实体转为视图对象，secret 仅在校验回显时传入。 */
    private static SourceView view(SocialIdentitySourceEntity s, String secret) {
        return new SourceView(s.id(), s.tenantId(), s.name(), s.type(), s.mode(), s.clientId(), secret,
                s.jitProvisioning(), s.status(), s.createdAt(), s.updatedAt());
    }

    /** 校验回调地址：必须为 http/https 绝对 URI 且不含 fragment，防止开放重定向。 */
    private static URI safeRedirect(String v) {
        try {
            URI u = URI.create(v);
            if (!u.isAbsolute() || u.getFragment() != null || !("https".equals(u.getScheme()) || "http".equals(u.getScheme()))) {
                throw new IllegalArgumentException();
            }
            return u;
        } catch (RuntimeException ex) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_REDIRECT_INVALID);
        }
    }

    /** 生成 n 字节随机数的 URL-safe Base64 字符串（用于 state/nonce/PKCE）。 */
    private String token(int n) {
        byte[] b = new byte[n];
        random.nextBytes(b);
        return base64(b);
    }

    /** SHA-256 摘要。 */
    private static byte[] sha256(String v) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(Objects.toString(v, "").getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** SHA-256 十六进制编码（用于 state/nonce/code 哈希比对）。 */
    private static String hash(String v) { return HexFormat.of().formatHex(sha256(v)); }

    /** URL-safe 无填充 Base64 编码。 */
    private static String base64(byte[] b) { return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    /** 净化第三方返回的用户名：仅保留字母数字、截断至 80 字符，空则生成随机占位。 */
    private static String sanitizeUsername(String raw) {
        String clean = raw.replaceAll("[^A-Za-z0-9]", "");
        if (clean.length() > 80) {
            clean = clean.substring(0, 80);
        }
        return clean.isBlank() ? "social_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) : clean;
    }

    // ==================== 值对象 ====================

    

    

    

    

    
}
