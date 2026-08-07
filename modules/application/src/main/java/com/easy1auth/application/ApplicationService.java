package com.easy1auth.application;

import com.easy1auth.application.model.*;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.tenant.TenantContextHolder;
import com.easy1auth.tenant.TenantService;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class ApplicationService {
    private static final OAuthApplicationEntityTable APP = OAuthApplicationEntityTable.$;
    private static final Set<String> TYPES = Set.of("web", "native", "spa", "machine");
    private static final Set<String> GRANTS = Set.of("authorization_code", "refresh_token", "client_credentials");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JSqlClient sql;
    private final TenantService tenants;
    private final PasswordEncoder passwords;

    public ApplicationService(JSqlClient sql, TenantService tenants, PasswordEncoder passwords) {
        this.sql = sql;
        this.tenants = tenants;
        this.passwords = passwords;
    }

    @Transactional
    public ApplicationView create(ApplicationInput input) {
        UUID tenant = TenantContextHolder.requireTenantId();
        var normalized = normalize(input, true);
        int limit = tenants.lockForAppQuota(tenant);
        long count = sql.createQuery(APP).select(APP.id()).fetchUnlimitedCount();
        if (count >= limit) throw new DomainException(ErrorCodeConstants.TENANT_APP_LIMIT);
        if (sql.createQuery(APP).where(APP.name().eq(normalized.name())).select(APP.id()).exists())
            throw new DomainException(ErrorCodeConstants.APPLICATION_NAME_EXISTS);
        UUID id = UuidV7.randomUuid();
        String clientId = "app_" + id.toString().replace("-", "");
        String secret = isPublic(normalized.type()) ? null : secret();
        Instant now = Instant.now();
        var entity = OAuthApplicationEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant).setName(normalized.name()).setLogo(normalized.logo()).setDescription(normalized.description()).setType(normalized.type()).setClientId(clientId).setClientSecretHash(secret == null ? null : passwords.encode(secret)).setRedirectUris(normalized.redirectUris()).setPostLogoutRedirectUris(normalized.postLogoutRedirectUris()).setAllowedGrantTypes(normalized.allowedGrantTypes()).setScopes(normalized.scopes()).setRequirePkce(normalized.requirePkce()).setRequireConsent(normalized.requireConsent()).setAccessTokenLifetime(normalized.accessTokenLifetime()).setRefreshTokenLifetime(normalized.refreshTokenLifetime()).setStatus("active").setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
        return view(entity, secret);
    }

    @Transactional(readOnly = true)
    public PageData<ApplicationView> list(int page, int pageSize, String name, String type, String status) {
        int p = Math.max(1, page), size = Math.min(100, Math.max(1, pageSize));
        var query = sql.createQuery(APP)
                .whereIf(name != null && !name.isBlank(), () -> APP.name().ilike(name, LikeMode.ANYWHERE))
                .whereIf(type != null && !type.isBlank(), () -> APP.type().eq(type))
                .whereIf(status != null && !status.isBlank(), () -> APP.status().eq(status))
                .orderBy(APP.createdAt().desc()).select(APP);
        long total = query.fetchUnlimitedCount();
        return PageData.of(query.limit(size, (long) (p - 1) * size).execute().stream().map(e -> view(e, null)).toList(), p, size, total);
    }

    @Transactional(readOnly = true)
    public ApplicationView get(UUID id) {
        return view(entity(id), null);
    }

    @Transactional(readOnly = true)
    public OAuthApplicationEntity findActiveByClientId(String clientId) {
        return sql.createQuery(APP).where(APP.clientId().eq(clientId), APP.status().eq("active")).select(APP).fetchOneOrNull();
    }

    @Transactional(readOnly = true)
    public OAuthApplicationEntity findActive(UUID tenant, UUID id) {
        return sql.createQuery(APP).where(APP.id().eq(id), APP.tenantId().eq(tenant), APP.status().eq("active")).select(APP).fetchOneOrNull();
    }

    @Transactional
    public ApplicationView update(UUID id, ApplicationInput input) {
        UUID tenant = TenantContextHolder.requireTenantId();
        var old = entity(id);
        var normalized = normalizeForUpdate(old, input);
        if (!old.name().equals(normalized.name()) && sql.createQuery(APP).where(APP.name().eq(normalized.name()), APP.id().ne(id)).select(APP.id()).exists())
            throw new DomainException(ErrorCodeConstants.APPLICATION_NAME_EXISTS);
        String oneTimeSecret = null;
        var update = sql.createUpdate(APP).set(APP.name(), normalized.name()).set(APP.logo(), normalized.logo()).set(APP.description(), normalized.description()).set(APP.type(), normalized.type()).set(APP.redirectUris(), normalized.redirectUris()).set(APP.postLogoutRedirectUris(), normalized.postLogoutRedirectUris()).set(APP.allowedGrantTypes(), normalized.allowedGrantTypes()).set(APP.scopes(), normalized.scopes()).set(APP.requirePkce(), normalized.requirePkce()).set(APP.requireConsent(), normalized.requireConsent()).set(APP.accessTokenLifetime(), normalized.accessTokenLifetime()).set(APP.refreshTokenLifetime(), normalized.refreshTokenLifetime()).set(APP.updatedAt(), Instant.now()).where(APP.id().eq(id), APP.tenantId().eq(tenant));
        if (isPublic(old.type()) && !isPublic(normalized.type())) {
            oneTimeSecret = secret();
            update.set(APP.clientSecretHash(), passwords.encode(oneTimeSecret));
        } else if (!isPublic(old.type()) && isPublic(normalized.type()))
            update.set(APP.clientSecretHash(), (String) null);
        update.execute();
        return view(entity(id), oneTimeSecret);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenant = TenantContextHolder.requireTenantId();
        if (sql.createDelete(APP).where(APP.id().eq(id), APP.tenantId().eq(tenant)).execute() != 1) throw missing();
    }

    @Transactional
    public ApplicationView status(UUID id, String status) {
        UUID tenant = TenantContextHolder.requireTenantId();
        if (!Set.of("active", "disabled").contains(status))
            throw new DomainException(ErrorCodeConstants.APPLICATION_STATUS_INVALID);
        entity(id);
        sql.createUpdate(APP).set(APP.status(), status).set(APP.updatedAt(), Instant.now()).where(APP.id().eq(id), APP.tenantId().eq(tenant)).execute();
        return get(id);
    }

    @Transactional
    public SecretView regenerateSecret(UUID id) {
        UUID tenant = TenantContextHolder.requireTenantId();
        var app = entity(id);
        if (isPublic(app.type()))
            throw new DomainException(ErrorCodeConstants.PUBLIC_CLIENT_HAS_NO_SECRET);
        String secret = secret();
        sql.createUpdate(APP).set(APP.clientSecretHash(), passwords.encode(secret)).set(APP.updatedAt(), Instant.now()).where(APP.id().eq(id), APP.tenantId().eq(tenant)).execute();
        return new SecretView(secret);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        var rows = sql.createQuery(APP).select(APP.status()).execute();
        return Map.of("totalApplications", (long) rows.size(), "activeApplications", rows.stream().filter("active"::equals).count(), "disabledApplications", rows.stream().filter("disabled"::equals).count());
    }

    private OAuthApplicationEntity entity(UUID id) {
        return sql.createQuery(APP).where(APP.id().eq(id)).select(APP).fetchOptional().orElseThrow(this::missing);
    }

    private DomainException missing() {
        return new DomainException(ErrorCodeConstants.APPLICATION_NOT_FOUND);
    }

    private static boolean isPublic(String type) {
        return "spa".equals(type) || "native".equals(type);
    }

    private static String secret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void uri(String value) {
        try {
            URI u = URI.create(value);
            if (!u.isAbsolute() || u.getHost() == null || u.getFragment() != null || !("https".equalsIgnoreCase(u.getScheme()) || "http".equalsIgnoreCase(u.getScheme())))
                throw new IllegalArgumentException();
        } catch (RuntimeException ex) {
            throw new DomainException(ErrorCodeConstants.REDIRECT_URI_INVALID);
        }
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).map(String::strip).filter(v -> !v.isEmpty()).distinct().toList();
    }

    private static ApplicationInput normalize(ApplicationInput in, boolean creating) {
        if (in == null || in.name() == null || in.name().isBlank())
            throw new DomainException(ErrorCodeConstants.APPLICATION_NAME_REQUIRED);
        String type = in.type() == null ? "web" : in.type();
        if (!TYPES.contains(type)) throw new DomainException(ErrorCodeConstants.APPLICATION_TYPE_INVALID);
        List<String> redirects = copy(in.redirectUris());
        redirects.forEach(ApplicationService::uri);
        List<String> logout = copy(in.postLogoutRedirectUris());
        logout.forEach(ApplicationService::uri);
        List<String> grants = in.allowedGrantTypes() == null ? defaultGrants(type) : copy(in.allowedGrantTypes());
        if (grants.isEmpty() || !GRANTS.containsAll(grants))
            throw new DomainException(ErrorCodeConstants.GRANT_TYPE_INVALID);
        if (isPublic(type) && grants.contains("client_credentials"))
            throw new DomainException(ErrorCodeConstants.PUBLIC_CLIENT_GRANT_INVALID);
        List<String> scopes = in.scopes() == null ? List.of("openid", "profile", "email", "phone") : copy(in.scopes());
        int access = in.accessTokenLifetime() == null ? 900 : in.accessTokenLifetime(), refresh = in.refreshTokenLifetime() == null ? 2592000 : in.refreshTokenLifetime();
        if (access < 60 || access > 86400 || refresh < 300 || refresh > 31536000)
            throw new DomainException(ErrorCodeConstants.TOKEN_LIFETIME_INVALID);
        boolean pkce = in.requirePkce() != null ? in.requirePkce() : grants.contains("authorization_code");
        if (isPublic(type) && grants.contains("authorization_code") && !pkce)
            throw new DomainException(ErrorCodeConstants.PKCE_REQUIRED);
        return new ApplicationInput(in.name().strip(), in.logo(), in.description(), type, redirects, logout, grants, scopes, pkce, in.requireConsent() == null || in.requireConsent(), access, refresh);
    }

    private static List<String> defaultGrants(String type) {
        return "machine".equals(type) ? List.of("client_credentials") : List.of("authorization_code", "refresh_token");
    }

    private static ApplicationInput normalizeForUpdate(OAuthApplicationEntity old, ApplicationInput in) {
        if (in == null) return from(old);
        return normalize(new ApplicationInput(in.name() == null ? old.name() : in.name(), in.logo() == null ? old.logo() : in.logo(), in.description() == null ? old.description() : in.description(), in.type() == null ? old.type() : in.type(), in.redirectUris() == null ? old.redirectUris() : in.redirectUris(), in.postLogoutRedirectUris() == null ? old.postLogoutRedirectUris() : in.postLogoutRedirectUris(), in.allowedGrantTypes() == null ? old.allowedGrantTypes() : in.allowedGrantTypes(), in.scopes() == null ? old.scopes() : in.scopes(), in.requirePkce() == null ? old.requirePkce() : in.requirePkce(), in.requireConsent() == null ? old.requireConsent() : in.requireConsent(), in.accessTokenLifetime() == null ? old.accessTokenLifetime() : in.accessTokenLifetime(), in.refreshTokenLifetime() == null ? old.refreshTokenLifetime() : in.refreshTokenLifetime()), false);
    }

    private static ApplicationInput from(OAuthApplicationEntity e) {
        return new ApplicationInput(e.name(), e.logo(), e.description(), e.type(), e.redirectUris(), e.postLogoutRedirectUris(), e.allowedGrantTypes(), e.scopes(), e.requirePkce(), e.requireConsent(), e.accessTokenLifetime(), e.refreshTokenLifetime());
    }

    private static ApplicationView view(OAuthApplicationEntity e, String secret) {
        return new ApplicationView(e.id(), e.tenantId(), e.name(), e.logo(), e.description(), e.type(), e.clientId(), secret, e.redirectUris(), e.postLogoutRedirectUris(), e.allowedGrantTypes(), e.scopes(), e.requirePkce(), e.requireConsent(), e.accessTokenLifetime(), e.refreshTokenLifetime(), e.status(), e.createdAt(), e.updatedAt());
    }

    public record ApplicationInput(String name, String logo, String description, String type, List<String> redirectUris,
                                   List<String> postLogoutRedirectUris, List<String> allowedGrantTypes,
                                   List<String> scopes, Boolean requirePkce, Boolean requireConsent,
                                   Integer accessTokenLifetime, Integer refreshTokenLifetime) {
    }

    public record ApplicationView(UUID id, UUID tenantId, String name, String logo, String description, String type,
                                  String clientId, String clientSecret, List<String> redirectUris,
                                  List<String> postLogoutRedirectUris, List<String> allowedGrantTypes,
                                  List<String> scopes, boolean requirePkce, boolean requireConsent,
                                  int accessTokenLifetime, int refreshTokenLifetime, String status, Instant createdAt,
                                  Instant updatedAt) {
    }

    public record SecretView(String clientSecret) {
    }
}
