package com.easy1auth.application.service;

import com.easy1auth.application.constant.ErrorCodeConstants;
import com.easy1auth.application.dto.ApplicationInput;
import com.easy1auth.application.dto.ApplicationStats;
import com.easy1auth.application.dto.ApplicationView;
import com.easy1auth.application.dto.SecretView;
import com.easy1auth.application.model.*;
import com.easy1auth.application.repository.ApplicationRepository;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.id.UuidV7;
import com.easy1auth.infrastructure.foundation.web.PageData;
import com.easy1auth.tenant.util.TenantContextHolder;
import com.easy1auth.tenant.service.TenantService;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * OAuth2 应用服务：租户下 OAuth 客户端应用的生命周期管理。
 *
 * <p>负责应用的创建、分页查询、详情、更新、删除、启停、密钥重新生成与统计，
 * 并对类型、授权类型、重定向 URI、令牌有效期等做归一化与合规校验；创建与更新
 * 会校验租户应用配额与应用名称的唯一性。</p>
 */
@Service
public class ApplicationService {
    /**
     * oauth_application 表静态描述符
     */
    private static final OAuthApplicationEntityTable APP = OAuthApplicationEntityTable.$;
    /**
     * 支持的应用类型：web（Web）/ native（原生）/ spa（单页应用）/ machine（机器）
     */
    private static final Set<String> TYPES = Set.of("web", "native", "spa", "machine");
    /**
     * 支持的授权类型：授权码 / 刷新令牌 / 客户端凭证
     */
    private static final Set<String> GRANTS = Set.of("authorization_code", "refresh_token", "client_credentials");
    /**
     * 安全随机数生成器（生成客户端密钥）
     */
    private static final SecureRandom RANDOM = new SecureRandom();
    /**
     * jimmer SQL 客户端
     */
    private final ApplicationRepository repository;
    /**
     * 租户服务（应用配额校验等）
     */
    private final TenantService tenants;
    /**
     * 密码编码器（编码客户端密钥哈希）
     */
    private final PasswordEncoder passwords;

    public ApplicationService(ApplicationRepository repository, TenantService tenants, PasswordEncoder passwords) {
        this.repository = repository;
        this.tenants = tenants;
        this.passwords = passwords;
    }

    /**
     * 创建 OAuth 应用：校验配额与名称唯一性，生成 client_id 与客户端密钥（公共客户端不生成），返回视图。
     */
    @Transactional
    public ApplicationView create(ApplicationInput input) {
        UUID tenant = TenantContextHolder.requireTenantId();
        var normalized = normalize(input, true);
        int limit = tenants.lockForAppQuota(tenant);
        long count = repository.count();
        if (count >= limit) {
            throw new DomainException(ErrorCodeConstants.TENANT_APP_LIMIT);
        }
        if (repository.sql().createQuery(APP).where(APP.name().eq(normalized.name())).select(APP.id()).exists()) {
            throw new DomainException(ErrorCodeConstants.APPLICATION_NAME_EXISTS);
        }
        UUID id = UuidV7.randomUuid();
        String clientId = "app_" + id.toString().replace("-", "");
        String secret = isPublic(normalized.type()) ? null : secret();
        Instant now = Instant.now();
        var entity = OAuthApplicationEntityDraft.$.produce(d ->
                d.setId(id)
                        .setTenantId(tenant)
                        .setName(normalized.name())
                        .setLogo(normalized.logo())
                        .setDescription(normalized.description())
                        .setType(normalized.type())
                        .setClientId(clientId)
                        .setClientSecretHash(secret == null ? null : passwords.encode(secret))
                        .setRedirectUris(normalized.redirectUris())
                        .setPostLogoutRedirectUris(normalized.postLogoutRedirectUris())
                        .setAllowedGrantTypes(normalized.allowedGrantTypes())
                        .setScopes(normalized.scopes()).setRequirePkce(normalized.requirePkce())
                        .setRequireConsent(normalized.requireConsent())
                        .setAccessTokenLifetime(normalized.accessTokenLifetime())
                        .setRefreshTokenLifetime(normalized.refreshTokenLifetime())
                        .setStatus("active")
                        .setCreatedAt(now)
                        .setUpdatedAt(now));
        repository.saveApplication(entity);
        return view(entity, secret);
    }

    /**
     * 分页查询应用列表，支持按名称（模糊）/类型/状态过滤，按创建时间倒序。
     */
    @Transactional(readOnly = true)
    public PageData<ApplicationView> list(int page, int pageSize, String name, String type, String status) {
        int p = Math.max(1, page), size = Math.min(100, Math.max(1, pageSize));
        var query = repository.sql().createQuery(APP)
                .whereIf(name != null && !name.isBlank(), () -> APP.name().ilike(name, LikeMode.ANYWHERE))
                .whereIf(type != null && !type.isBlank(), () -> APP.type().eq(type))
                .whereIf(status != null && !status.isBlank(), () -> APP.status().eq(status))
                .orderBy(APP.createdAt().desc()).select(APP);
        long total = query.fetchUnlimitedCount();
        return PageData.of(query.limit(size, (long) (p - 1) * size).execute().stream().map(e -> view(e, null)).toList(), p, size, total);
    }

    /**
     * 按 ID 查询应用详情（不含客户端密钥明文）。
     */
    @Transactional(readOnly = true)
    public ApplicationView get(UUID id) {
        return view(entity(id), null);
    }

    /**
     * 按客户端 ID 查询 active 状态的应用（供授权服务器使用）。
     */
    @Transactional(readOnly = true)
    public OAuthApplicationEntity findActiveByClientId(String clientId) {
        return repository.sql().createQuery(APP).where(APP.clientId().eq(clientId), APP.status().eq("active")).select(APP).fetchOneOrNull();
    }

    /**
     * 按 ID 查询指定租户下 active 状态的应用（供授权服务器使用）。
     */
    @Transactional(readOnly = true)
    public OAuthApplicationEntity findActive(UUID tenant, UUID id) {
        return repository.sql().createQuery(APP).where(APP.id().eq(id), APP.tenantId().eq(tenant), APP.status().eq("active")).select(APP).fetchOneOrNull();
    }

    /**
     * 更新应用配置（未传字段沿用旧值），并在公开/机密客户端类型切换时生成或清空密钥。
     */
    @Transactional
    public ApplicationView update(UUID id, ApplicationInput input) {
        UUID tenant = TenantContextHolder.requireTenantId();
        var old = entity(id);
        var normalized = normalizeForUpdate(old, input);
        if (!old.name().equals(normalized.name()) && repository.sql().createQuery(APP).where(APP.name().eq(normalized.name()), APP.id().ne(id)).select(APP.id()).exists()) {
            throw new DomainException(ErrorCodeConstants.APPLICATION_NAME_EXISTS);
        }
        String oneTimeSecret = null;
        var update = repository.sql().createUpdate(APP).set(APP.name(), normalized.name()).set(APP.logo(), normalized.logo()).set(APP.description(), normalized.description()).set(APP.type(), normalized.type()).set(APP.redirectUris(), normalized.redirectUris()).set(APP.postLogoutRedirectUris(), normalized.postLogoutRedirectUris()).set(APP.allowedGrantTypes(), normalized.allowedGrantTypes()).set(APP.scopes(), normalized.scopes()).set(APP.requirePkce(), normalized.requirePkce()).set(APP.requireConsent(), normalized.requireConsent()).set(APP.accessTokenLifetime(), normalized.accessTokenLifetime()).set(APP.refreshTokenLifetime(), normalized.refreshTokenLifetime()).set(APP.updatedAt(), Instant.now()).where(APP.id().eq(id), APP.tenantId().eq(tenant));
        if (isPublic(old.type()) && !isPublic(normalized.type())) {
            oneTimeSecret = secret();
            update.set(APP.clientSecretHash(), passwords.encode(oneTimeSecret));
        } else if (!isPublic(old.type()) && isPublic(normalized.type())) {
            update.set(APP.clientSecretHash(), (String) null);
        }
        update.execute();
        return view(entity(id), oneTimeSecret);
    }

    /**
     * 删除应用（物理删除，仅限当前租户内）。
     */
    @Transactional
    public void delete(UUID id) {
        UUID tenant = TenantContextHolder.requireTenantId();
        repository.delete(tenant,id);
    }

    /**
     * 启停应用：更新状态为 active/disabled。
     */
    @Transactional
    public ApplicationView status(UUID id, String status) {
        UUID tenant = TenantContextHolder.requireTenantId();
        if (!Set.of("active", "disabled").contains(status)) {
            throw new DomainException(ErrorCodeConstants.APPLICATION_STATUS_INVALID);
        }
        entity(id);
        repository.sql().createUpdate(APP).set(APP.status(), status).set(APP.updatedAt(), Instant.now()).where(APP.id().eq(id), APP.tenantId().eq(tenant)).execute();
        return get(id);
    }

    /**
     * 重新生成客户端密钥（公共客户端不允许），返回一次性明文密钥。
     */
    @Transactional
    public SecretView regenerateSecret(UUID id) {
        UUID tenant = TenantContextHolder.requireTenantId();
        var app = entity(id);
        if (isPublic(app.type())) {
            throw new DomainException(ErrorCodeConstants.PUBLIC_CLIENT_HAS_NO_SECRET);
        }
        String secret = secret();
        repository.sql().createUpdate(APP).set(APP.clientSecretHash(), passwords.encode(secret)).set(APP.updatedAt(), Instant.now()).where(APP.id().eq(id), APP.tenantId().eq(tenant)).execute();
        return new SecretView(secret);
    }

    /**
     * 统计应用总量及 active/disabled 数量（租户维度）。
     */
    @Transactional(readOnly = true)
    public ApplicationStats stats() {
        var rows = repository.sql().createQuery(APP).select(APP.status()).execute();
        return new ApplicationStats(rows.size(), rows.stream().filter("active"::equals).count(), rows.stream().filter("disabled"::equals).count());
    }


    /**
     * 按 ID 查询应用，不存在时抛出领域异常。
     */
    private OAuthApplicationEntity entity(UUID id) {
        return repository.sql().createQuery(APP).where(APP.id().eq(id)).select(APP).fetchOptional().orElseThrow(this::missing);
    }

    /**
     * 构造“应用不存在”异常。
     */
    private DomainException missing() {
        return new DomainException(ErrorCodeConstants.APPLICATION_NOT_FOUND);
    }

    /**
     * 判断应用类型是否为公共客户端（spa/native），公共客户端不持有密钥。
     */
    private static boolean isPublic(String type) {
        return "spa".equals(type) || "native".equals(type);
    }

    /**
     * 生成 32 字节随机客户端密钥（Base64 URL 编码，无填充）。
     */
    private static String secret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 校验重定向 URI：必须为无 fragment 的绝对 HTTP(S) URI。
     */
    private static void uri(String value) {
        try {
            URI u = URI.create(value);
            if (!u.isAbsolute() || u.getHost() == null || u.getFragment() != null || !("https".equalsIgnoreCase(u.getScheme()) || "http".equalsIgnoreCase(u.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException ex) {
            throw new DomainException(ErrorCodeConstants.REDIRECT_URI_INVALID);
        }
    }

    /**
     * 规范化字符串列表：去空白、剔除空串、去重。
     */
    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).map(String::strip).filter(v -> !v.isEmpty()).distinct().toList();
    }

    /**
     * 归一化并校验创建/更新的应用输入，返回规范化后的输入。
     */
    private static ApplicationInput normalize(ApplicationInput in, boolean creating) {
        if (in == null || in.name() == null || in.name().isBlank()) {
            throw new DomainException(ErrorCodeConstants.APPLICATION_NAME_REQUIRED);
        }
        String type = in.type() == null ? "web" : in.type();
        if (!TYPES.contains(type)) {
            throw new DomainException(ErrorCodeConstants.APPLICATION_TYPE_INVALID);
        }
        List<String> redirects = copy(in.redirectUris());
        redirects.forEach(ApplicationService::uri);
        List<String> logout = copy(in.postLogoutRedirectUris());
        logout.forEach(ApplicationService::uri);
        List<String> grants = in.allowedGrantTypes() == null ? defaultGrants(type) : copy(in.allowedGrantTypes());
        if (grants.isEmpty() || !GRANTS.containsAll(grants)) {
            throw new DomainException(ErrorCodeConstants.GRANT_TYPE_INVALID);
        }
        if (isPublic(type) && grants.contains("client_credentials")) {
            throw new DomainException(ErrorCodeConstants.PUBLIC_CLIENT_GRANT_INVALID);
        }
        List<String> scopes = in.scopes() == null ? List.of("openid", "profile", "email", "phone") : copy(in.scopes());
        int access = in.accessTokenLifetime() == null ? 900 : in.accessTokenLifetime(), refresh = in.refreshTokenLifetime() == null ? 2592000 : in.refreshTokenLifetime();
        if (access < 60 || access > 86400 || refresh < 300 || refresh > 31536000) {
            throw new DomainException(ErrorCodeConstants.TOKEN_LIFETIME_INVALID);
        }
        boolean pkce = in.requirePkce() != null ? in.requirePkce() : grants.contains("authorization_code");
        if (isPublic(type) && grants.contains("authorization_code") && !pkce) {
            throw new DomainException(ErrorCodeConstants.PKCE_REQUIRED);
        }
        return new ApplicationInput(in.name().strip(), in.logo(), in.description(), type, redirects, logout, grants, scopes, pkce, in.requireConsent() == null || in.requireConsent(), access, refresh);
    }

    /**
     * 返回应用类型对应的默认授权类型（machine 为 client_credentials，其余为授权码+刷新令牌）。
     */
    private static List<String> defaultGrants(String type) {
        return "machine".equals(type) ? List.of("client_credentials") : List.of("authorization_code", "refresh_token");
    }

    /**
     * 用传入输入覆盖旧值（未传字段沿用旧值）后进行归一化校验。
     */
    private static ApplicationInput normalizeForUpdate(OAuthApplicationEntity old, ApplicationInput in) {
        if (in == null) {
            return from(old);
        }
        return normalize(new ApplicationInput(in.name() == null ? old.name() : in.name(), in.logo() == null ? old.logo() : in.logo(), in.description() == null ? old.description() : in.description(), in.type() == null ? old.type() : in.type(), in.redirectUris() == null ? old.redirectUris() : in.redirectUris(), in.postLogoutRedirectUris() == null ? old.postLogoutRedirectUris() : in.postLogoutRedirectUris(), in.allowedGrantTypes() == null ? old.allowedGrantTypes() : in.allowedGrantTypes(), in.scopes() == null ? old.scopes() : in.scopes(), in.requirePkce() == null ? old.requirePkce() : in.requirePkce(), in.requireConsent() == null ? old.requireConsent() : in.requireConsent(), in.accessTokenLifetime() == null ? old.accessTokenLifetime() : in.accessTokenLifetime(), in.refreshTokenLifetime() == null ? old.refreshTokenLifetime() : in.refreshTokenLifetime()), false);
    }

    /**
     * 将应用实体转换为输入对象（用于整体更新）。
     */
    private static ApplicationInput from(OAuthApplicationEntity e) {
        return new ApplicationInput(e.name(), e.logo(), e.description(), e.type(), e.redirectUris(), e.postLogoutRedirectUris(), e.allowedGrantTypes(), e.scopes(), e.requirePkce(), e.requireConsent(), e.accessTokenLifetime(), e.refreshTokenLifetime());
    }

    /**
     * 将应用实体转换为视图，附带可选的客户端密钥明文。
     */
    private static ApplicationView view(OAuthApplicationEntity e, String secret) {
        return new ApplicationView(e.id(), e.tenantId(), e.name(), e.logo(), e.description(), e.type(), e.clientId(), secret, e.redirectUris(), e.postLogoutRedirectUris(), e.allowedGrantTypes(), e.scopes(), e.requirePkce(), e.requireConsent(), e.accessTokenLifetime(), e.refreshTokenLifetime(), e.status(), e.createdAt(), e.updatedAt());
    }


}
