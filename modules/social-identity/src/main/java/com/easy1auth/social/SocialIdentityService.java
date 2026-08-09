package com.easy1auth.social;

import com.easy1auth.directory.PoolUserService;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.security.SecurityDataCipher;
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
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 社会化身份源服务：管理身份源 CRUD，并编排社交登录 authorize/callback 流程。
 *
 * <p>厂商协议（授权 URL、token 交换、用户信息拉取）委托给 {@link SocialIdentityAdapter}，
 * 厂商无关逻辑（state/nonce 生成、事务存取、绑定 upsert、JIT 开户）在本类内统一处理。</p>
 */
@Service
public class SocialIdentityService {

    private static final SocialIdentitySourceEntityTable SOURCE = SocialIdentitySourceEntityTable.$;
    private static final SocialLoginTransactionEntityTable TX = SocialLoginTransactionEntityTable.$;
    private static final SocialIdentityBindingEntityTable BINDING = SocialIdentityBindingEntityTable.$;

    private final JSqlClient sql;
    private final SecurityDataCipher cipher;
    private final PoolUserService users;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, SocialIdentityAdapter> adapters;

    public SocialIdentityService(JSqlClient sql, SecurityDataCipher cipher, PoolUserService users, List<SocialIdentityAdapter> adapterList) {
        this.sql = sql;
        this.cipher = cipher;
        this.users = users;
        Map<String, SocialIdentityAdapter> map = new LinkedHashMap<>();
        for (var a : adapterList) map.put(a.type(), a);
        this.adapters = Map.copyOf(map);
    }

    // ==================== 管理 API ====================

    @Transactional
    public SourceView create(UUID tenant, Input in) {
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

    @Transactional(readOnly = true)
    public SourceView get(UUID tenant, UUID id) {
        return view(entity(tenant, id), null);
    }

    @Transactional
    public SourceView update(UUID tenant, UUID id, Input in) {
        var old = entity(tenant, id);
        validate(in, false);
        var u = sql.createUpdate(SOURCE).set(SOURCE.updatedAt(), Instant.now())
                .where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id));
        if (in.name() != null) u.set(SOURCE.name(), in.name().strip());
        if (in.type() != null) u.set(SOURCE.type(), in.type());
        if (in.mode() != null) u.set(SOURCE.mode(), in.mode());
        if (in.clientId() != null) u.set(SOURCE.clientId(), in.clientId().strip());
        if (in.clientSecret() != null && !in.clientSecret().isBlank())
            u.set(SOURCE.encryptedClientSecret(), cipher.encrypt("social:" + tenant + ":" + id, in.clientSecret()));
        if (in.jitProvisioning() != null) u.set(SOURCE.jitProvisioning(), in.jitProvisioning());
        if (in.status() != null) u.set(SOURCE.status(), status(in.status()));
        u.execute();
        return get(tenant, id);
    }

    @Transactional
    public void delete(UUID tenant, UUID id) {
        if (sql.createDelete(SOURCE).where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id)).execute() != 1)
            throw missing();
    }

    /** 查询租户下已启用的身份源（供登录页渲染按钮用）。 */
    @Transactional(readOnly = true)
    public List<SourceView> listActive(UUID tenant) {
        return sql.createQuery(SOURCE).where(SOURCE.tenantId().eq(tenant), SOURCE.status().eq("active"))
                .orderBy(SOURCE.createdAt().asc()).select(SOURCE).execute().stream()
                .map(e -> view(e, null)).toList();
    }

    // ==================== 登录流程 ====================

    @Transactional
    public AuthorizationStart authorize(UUID tenant, UUID sourceId, String redirectUri) {
        var s = entity(tenant, sourceId);
        if (!"active".equals(s.status())) throw new DomainException(ErrorCodeConstants.SOCIAL_SOURCE_DISABLED);
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

    @Transactional
    public LoginResult callback(UUID tenant, UUID sourceId, String code, String state, String redirectUri) {
        var tx = sql.createQuery(TX).where(TX.tenantId().eq(tenant), TX.sourceId().eq(sourceId), TX.stateHash().eq(hash(state)))
                .select(TX).forUpdate().fetchOneOrNull();
        if (tx == null || tx.consumedAt() != null || tx.expiresAt().isBefore(Instant.now()) || !Objects.equals(tx.returnUri(), redirectUri))
            throw new DomainException(ErrorCodeConstants.SOCIAL_TRANSACTION_INVALID);
        var s = entity(tenant, sourceId);
        var adapter = adapter(s.type());
        String secret = cipher.decrypt("social:" + tenant + ":" + sourceId, s.encryptedClientSecret());
        String verifier = tx.encryptedPkceVerifier() != null
                ? cipher.decrypt("social-tx:" + tx.id() + ":pkce", tx.encryptedPkceVerifier()) : null;
        var info = adapter.exchangeAndFetch(s.clientId(), secret, code, safeRedirect(redirectUri), verifier);

        var binding = sql.createQuery(BINDING)
                .where(BINDING.tenantId().eq(tenant), BINDING.sourceId().eq(sourceId), BINDING.subject().eq(info.subject()))
                .select(BINDING).fetchOneOrNull();
        boolean created = false;
        UUID userId;
        if (binding != null) {
            userId = binding.poolUserId();
            sql.createUpdate(BINDING).set(BINDING.lastLoginAt(), Instant.now()).where(BINDING.id().eq(binding.id())).execute();
        } else {
            if (!s.jitProvisioning()) throw new DomainException(ErrorCodeConstants.SOCIAL_BINDING_REQUIRED);
            String email = info.email();
            if (email == null) throw new DomainException(ErrorCodeConstants.SOCIAL_VERIFIED_EMAIL_REQUIRED);
            String name = info.name() != null ? info.name() : email;
            String username = sanitizeUsername(s.type() + "_" + info.subject());
            var user = users.create(tenant, new PoolUserService.Input(username, email, null, null, name, info.avatar(),
                    null, null, null, Map.of("federated", true, "social_type", s.type())));
            userId = user.id();
            created = true;
            var e = SocialIdentityBindingEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant)
                    .setSourceId(sourceId).setPoolUserId(userId).setSourceType(s.type()).setSubject(info.subject())
                    .setClaims(Map.of("name", name, "email", email, "username", info.username() != null ? info.username() : ""))
                    .setCreatedAt(Instant.now()).setLastLoginAt(Instant.now()));
            sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        }
        sql.createUpdate(TX).set(TX.consumedAt(), Instant.now()).where(TX.id().eq(tx.id()), TX.consumedAt().isNull()).execute();
        return new LoginResult(userId, created);
    }

    // ==================== TenantContextHolder 便捷重载 ====================

    @Transactional
    public SourceView create(Input in) { return create(TenantContextHolder.requireTenantId(), in); }

    @Transactional(readOnly = true)
    public PageData<SourceView> list(int page, int size, String search, String status) {
        return list(TenantContextHolder.requireTenantId(), page, size, search, status);
    }

    @Transactional(readOnly = true)
    public SourceView get(UUID id) { return get(TenantContextHolder.requireTenantId(), id); }

    @Transactional
    public SourceView update(UUID id, Input in) { return update(TenantContextHolder.requireTenantId(), id, in); }

    @Transactional
    public void delete(UUID id) { delete(TenantContextHolder.requireTenantId(), id); }

    @Transactional(readOnly = true)
    public List<SourceView> listActive() { return listActive(TenantContextHolder.requireTenantId()); }

    // ==================== 私有辅助 ====================

    private SocialIdentityAdapter adapter(String type) {
        var a = adapters.get(type);
        if (a == null) throw new DomainException(ErrorCodeConstants.SOCIAL_TYPE_NOT_SUPPORTED);
        return a;
    }

    private SocialIdentitySourceEntity entity(UUID tenant, UUID id) {
        return sql.createQuery(SOURCE).where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id)).select(SOURCE)
                .fetchOptional().orElseThrow(this::missing);
    }

    private DomainException missing() { return new DomainException(ErrorCodeConstants.SOCIAL_SOURCE_NOT_FOUND); }

    private void validate(Input i, boolean create) {
        if (i == null || (create && (blank(i.name()) || blank(i.type()) || blank(i.clientId()) || blank(i.clientSecret()))))
            throw new DomainException(ErrorCodeConstants.SOCIAL_SOURCE_INVALID);
        if (i.type() != null && !adapters.containsKey(i.type()))
            throw new DomainException(ErrorCodeConstants.SOCIAL_TYPE_NOT_SUPPORTED);
        if (i.status() != null) status(i.status());
    }

    private static String status(String s) {
        if (!Set.of("active", "disabled").contains(s))
            throw new DomainException(ErrorCodeConstants.SOCIAL_STATUS_INVALID);
        return s;
    }

    private static SourceView view(SocialIdentitySourceEntity s, String secret) {
        return new SourceView(s.id(), s.tenantId(), s.name(), s.type(), s.mode(), s.clientId(), secret,
                s.jitProvisioning(), s.status(), s.createdAt(), s.updatedAt());
    }

    private static URI safeRedirect(String v) {
        try {
            URI u = URI.create(v);
            if (!u.isAbsolute() || u.getFragment() != null || !("https".equals(u.getScheme()) || "http".equals(u.getScheme())))
                throw new IllegalArgumentException();
            return u;
        } catch (RuntimeException ex) {
            throw new DomainException(ErrorCodeConstants.SOCIAL_REDIRECT_INVALID);
        }
    }

    private String token(int n) {
        byte[] b = new byte[n];
        random.nextBytes(b);
        return base64(b);
    }

    private static byte[] sha256(String v) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(Objects.toString(v, "").getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hash(String v) { return HexFormat.of().formatHex(sha256(v)); }

    private static String base64(byte[] b) { return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private static String sanitizeUsername(String raw) {
        String clean = raw.replaceAll("[^A-Za-z0-9]", "");
        if (clean.length() > 80) clean = clean.substring(0, 80);
        return clean.isBlank() ? "social_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) : clean;
    }

    // ==================== 值对象 ====================

    public record Input(String name, String type, String mode, String clientId, String clientSecret,
                        Boolean jitProvisioning, String status) { }

    public record SourceView(UUID id, UUID tenantId, String name, String type, String mode, String clientId,
                             String clientSecret, boolean jitProvisioning, String status,
                             Instant createdAt, Instant updatedAt) { }

    public record AuthorizationStart(String authorizeUrl, String state, int expiresIn) { }

    public record LoginResult(UUID poolUserId, boolean isNewUser) { }
}
