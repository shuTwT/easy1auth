package com.easy1auth.federation;

import com.easy1auth.tenant.TenantContextHolder;
import com.easy1auth.directory.*;
import com.easy1auth.federation.model.*;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.security.SecurityDataCipher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class FederationService {
    private static final FederationProviderEntityTable PROVIDER = FederationProviderEntityTable.$;
    private static final FederationLoginTransactionEntityTable TX = FederationLoginTransactionEntityTable.$;
    private static final ExternalIdentityBindingEntityTable BINDING = ExternalIdentityBindingEntityTable.$;
    private final JSqlClient sql;
    private final SecurityDataCipher cipher;
    private final ObjectMapper json;
    private final PoolUserService users;
    private final SecureRandom random = new SecureRandom();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

    public FederationService(JSqlClient sql, SecurityDataCipher cipher, ObjectMapper json, PoolUserService users) {
        this.sql = sql;
        this.cipher = cipher;
        this.json = json;
        this.users = users;
    }

    @Transactional
    public ProviderView create(UUID tenant, Input in) {
        validate(in, true);
        UUID id = UuidV7.randomUuid();
        Instant now = Instant.now();
        var e = FederationProviderEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant).setName(in.name().strip()).setIssuer(normalizeIssuer(in.issuer())).setClientId(in.clientId().strip()).setEncryptedClientSecret(cipher.encrypt("oidc:" + tenant + ":" + id, in.clientSecret())).setScopes(scopes(in.scopes())).setClaimMapping(in.claimMapping() == null ? Map.of() : in.claimMapping()).setJitProvisioning(Boolean.TRUE.equals(in.jitProvisioning())).setStatus("active").setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return view(e, in.clientSecret());
    }

    @Transactional(readOnly = true)
    public PageData<ProviderView> list(UUID tenant, int page, int size, String search, String status) {
        int p = Math.max(1, page), s = Math.min(100, Math.max(1, size));
        var q = sql.createQuery(PROVIDER).where(PROVIDER.tenantId().eq(tenant)).whereIf(search != null && !search.isBlank(), () -> PROVIDER.name().ilike(search, LikeMode.ANYWHERE)).whereIf(status != null && !status.isBlank(), () -> PROVIDER.status().eq(status)).orderBy(PROVIDER.createdAt().desc()).select(PROVIDER);
        long total = q.fetchUnlimitedCount();
        return PageData.of(q.limit(s, (long) (p - 1) * s).execute().stream().map(e -> view(e, null)).toList(), p, s, total);
    }

    @Transactional(readOnly = true)
    public ProviderView get(UUID tenant, UUID id) {
        return view(entity(tenant, id), null);
    }

    @Transactional
    public ProviderView update(UUID tenant, UUID id, Input in) {
        var old = entity(tenant, id);
        validate(in, false);
        var u = sql.createUpdate(PROVIDER).set(PROVIDER.updatedAt(), Instant.now()).where(PROVIDER.tenantId().eq(tenant), PROVIDER.id().eq(id));
        if (in.name() != null) u.set(PROVIDER.name(), in.name().strip());
        if (in.issuer() != null) u.set(PROVIDER.issuer(), normalizeIssuer(in.issuer()));
        if (in.clientId() != null) u.set(PROVIDER.clientId(), in.clientId().strip());
        if (in.clientSecret() != null && !in.clientSecret().isBlank())
            u.set(PROVIDER.encryptedClientSecret(), cipher.encrypt("oidc:" + tenant + ":" + id, in.clientSecret()));
        if (in.scopes() != null) u.set(PROVIDER.scopes(), scopes(in.scopes()));
        if (in.claimMapping() != null) u.set(PROVIDER.claimMapping(), in.claimMapping());
        if (in.jitProvisioning() != null) u.set(PROVIDER.jitProvisioning(), in.jitProvisioning());
        if (in.status() != null) u.set(PROVIDER.status(), status(in.status()));
        u.execute();
        return get(tenant, id);
    }

    @Transactional
    public void delete(UUID tenant, UUID id) {
        if (sql.createDelete(PROVIDER).where(PROVIDER.tenantId().eq(tenant), PROVIDER.id().eq(id)).execute() != 1)
            throw missing();
    }

    @Transactional
    public AuthorizationStart authorize(UUID tenant, UUID providerId, String redirectUri) {
        var p = entity(tenant, providerId);
        if (!"active".equals(p.status())) throw new DomainException(ErrorCodeConstants.OIDC_PROVIDER_DISABLED);
        URI callback = safeRedirect(redirectUri);
        Map<String, Object> metadata = metadata(p.issuer());
        String endpoint = Objects.toString(metadata.get("authorization_endpoint"), "");
        requireHttpsEndpoint(endpoint, p.issuer());
        String state = token(32), nonce = token(32), verifier = token(48), challenge = base64(sha256(verifier));
        UUID id = UuidV7.randomUuid();
        Instant now = Instant.now();
        var tx = FederationLoginTransactionEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant).setProviderId(providerId).setStateHash(hash(state)).setNonceHash(hash(nonce)).setEncryptedNonce(cipher.encrypt("oidc-tx:" + id + ":nonce", nonce)).setEncryptedPkceVerifier(cipher.encrypt("oidc-tx:" + id + ":pkce", verifier)).setReturnUri(callback.toString()).setExpiresAt(now.plusSeconds(600)).setConsumedAt(null).setCreatedAt(now));
        sql.saveCommand(tx).setMode(SaveMode.INSERT_ONLY).execute();
        String url = endpoint + "?response_type=code&client_id=" + enc(p.clientId()) + "&redirect_uri=" + enc(callback.toString()) + "&scope=" + enc(String.join(" ", p.scopes())) + "&state=" + enc(state) + "&nonce=" + enc(nonce) + "&code_challenge=" + enc(challenge) + "&code_challenge_method=S256";
        return new AuthorizationStart(url, state, 600);
    }

    @Transactional
    public LoginResult callback(UUID tenant, UUID providerId, String code, String state, String redirectUri) {
        var tx = sql.createQuery(TX).where(TX.tenantId().eq(tenant), TX.providerId().eq(providerId), TX.stateHash().eq(hash(state))).select(TX).forUpdate().fetchOneOrNull();
        if (tx == null || tx.consumedAt() != null || tx.expiresAt().isBefore(Instant.now()) || !Objects.equals(tx.returnUri(), redirectUri))
            throw new DomainException(ErrorCodeConstants.OIDC_TRANSACTION_INVALID);
        var p = entity(tenant, providerId);
        Map<String, Object> metadata = metadata(p.issuer());
        String tokenEndpoint = Objects.toString(metadata.get("token_endpoint"), ""), jwks = Objects.toString(metadata.get("jwks_uri"), "");
        requireHttpsEndpoint(tokenEndpoint, p.issuer());
        requireHttpsEndpoint(jwks, p.issuer());
        String verifier = cipher.decrypt("oidc-tx:" + tx.id() + ":pkce", tx.encryptedPkceVerifier());
        Map<String, Object> tokens = postToken(tokenEndpoint, p, code, redirectUri, verifier);
        String raw = Objects.toString(tokens.get("id_token"), "");
        if (raw.isBlank()) throw new DomainException(ErrorCodeConstants.OIDC_ID_TOKEN_MISSING);
        JwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        Jwt jwt = decoder.decode(raw);
        String nonce = cipher.decrypt("oidc-tx:" + tx.id() + ":nonce", tx.encryptedNonce());
        if (!p.issuer().equals(jwt.getIssuer() == null ? null : jwt.getIssuer().toString()) || !jwt.getAudience().contains(p.clientId()) || !MessageDigest.isEqual(hash(nonce).getBytes(StandardCharsets.US_ASCII), hash(jwt.getClaimAsString("nonce")).getBytes(StandardCharsets.US_ASCII)))
            throw new DomainException(ErrorCodeConstants.OIDC_ID_TOKEN_INVALID);
        String sub = jwt.getSubject();
        var binding = sql.createQuery(BINDING).where(BINDING.tenantId().eq(tenant), BINDING.providerId().eq(providerId), BINDING.issuer().eq(p.issuer()), BINDING.subject().eq(sub)).select(BINDING).fetchOneOrNull();
        boolean created = false;
        UUID userId;
        if (binding != null) {
            userId = binding.poolUserId();
            sql.createUpdate(BINDING).set(BINDING.lastLoginAt(), Instant.now()).where(BINDING.id().eq(binding.id())).execute();
        } else {
            if (!p.jitProvisioning()) throw new DomainException(ErrorCodeConstants.OIDC_BINDING_REQUIRED);
            Boolean verified = jwt.getClaim("email_verified");
            String email = jwt.getClaimAsString("email");
            if (!Boolean.TRUE.equals(verified) || email == null)
                throw new DomainException(ErrorCodeConstants.OIDC_VERIFIED_EMAIL_REQUIRED);
            String name = Objects.toString(jwt.getClaimAsString("name"), email);
            String username = ("oidc_" + sub.replaceAll("[^A-Za-z0-9]", "")).substring(0, Math.min(80, ("oidc_" + sub.replaceAll("[^A-Za-z0-9]", "")).length()));
            var user = users.create(tenant, new PoolUserService.Input(username, email, null, null, name, null, null, null, null, Map.of("federated", true)));
            userId = user.id();
            created = true;
            var e = ExternalIdentityBindingEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setProviderId(providerId).setPoolUserId(userId).setIssuer(p.issuer()).setSubject(sub).setClaims(Map.of("email", email, "name", name)).setCreatedAt(Instant.now()).setLastLoginAt(Instant.now()));
            sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        }
        sql.createUpdate(TX).set(TX.consumedAt(), Instant.now()).where(TX.id().eq(tx.id()), TX.consumedAt().isNull()).execute();
        return new LoginResult(userId, created);
    }

    private Map<String, Object> metadata(String issuer) {
        try {
            URI uri = URI.create(issuer + "/.well-known/openid-configuration");
            guard(uri);
            var response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 1_000_000) throw new IOException();
            Map<String, Object> m = json.readValue(response.body(), new TypeReference<>() {
            });
            if (!issuer.equals(m.get("issuer")))
                throw new DomainException(ErrorCodeConstants.OIDC_DISCOVERY_INVALID);
            return m;
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException(ErrorCodeConstants.OIDC_DISCOVERY_FAILED);
        }
    }

    private Map<String, Object> postToken(String endpoint, FederationProviderEntity p, String code, String redirect, String verifier) {
        try {
            String secret = cipher.decrypt("oidc:" + p.tenantId() + ":" + p.id(), p.encryptedClientSecret());
            String body = "grant_type=authorization_code&code=" + enc(code) + "&redirect_uri=" + enc(redirect) + "&code_verifier=" + enc(verifier);
            var req = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(10)).header("Content-Type", "application/x-www-form-urlencoded").header("Authorization", "Basic " + Base64.getEncoder().encodeToString((p.clientId() + ":" + secret).getBytes(StandardCharsets.UTF_8))).POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200 || res.body().length() > 1_000_000) throw new IOException();
            return json.readValue(res.body(), new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new DomainException(ErrorCodeConstants.OIDC_TOKEN_EXCHANGE_FAILED);
        }
    }

    private FederationProviderEntity entity(UUID tenant, UUID id) {
        return sql.createQuery(PROVIDER).where(PROVIDER.tenantId().eq(tenant), PROVIDER.id().eq(id)).select(PROVIDER).fetchOptional().orElseThrow(this::missing);
    }

    private DomainException missing() {
        return new DomainException(ErrorCodeConstants.OIDC_PROVIDER_NOT_FOUND);
    }

    private void validate(Input i, boolean create) {
        if (i == null || (create && (blank(i.name()) || blank(i.issuer()) || blank(i.clientId()) || blank(i.clientSecret()))))
            throw new DomainException(ErrorCodeConstants.OIDC_PROVIDER_INVALID);
        if (i.issuer() != null) normalizeIssuer(i.issuer());
        if (i.status() != null) status(i.status());
    }

    private static String normalizeIssuer(String v) {
        try {
            URI u = URI.create(v.strip());
            if (u.getQuery() != null || u.getFragment() != null || u.getHost() == null || (!"https".equals(u.getScheme()) && !("http".equals(u.getScheme()) && Set.of("localhost", "127.0.0.1", "::1").contains(u.getHost()))))
                throw new IllegalArgumentException();
            String s = u.toString();
            return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
        } catch (RuntimeException ex) {
            throw new DomainException(ErrorCodeConstants.OIDC_ISSUER_INVALID);
        }
    }

    private static void guard(URI uri) throws Exception {
        if (uri.getHost() == null) throw new IllegalArgumentException();
        for (var a : java.net.InetAddress.getAllByName(uri.getHost()))
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress())
                throw new DomainException(ErrorCodeConstants.OIDC_ENDPOINT_FORBIDDEN);
    }

    private static void requireHttpsEndpoint(String value, String issuer) {
        try {
            URI u = URI.create(value);
            if (!"https".equals(u.getScheme()) && !issuer.startsWith("http://localhost") && !issuer.startsWith("http://127.0.0.1"))
                throw new IllegalArgumentException();
            guard(u);
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException(ErrorCodeConstants.OIDC_ENDPOINT_INVALID);
        }
    }

    private static URI safeRedirect(String v) {
        try {
            URI u = URI.create(v);
            if (!u.isAbsolute() || u.getFragment() != null || !("https".equals(u.getScheme()) || "http".equals(u.getScheme())))
                throw new IllegalArgumentException();
            return u;
        } catch (RuntimeException ex) {
            throw new DomainException(ErrorCodeConstants.OIDC_REDIRECT_INVALID);
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

    private static String hash(String v) {
        return HexFormat.of().formatHex(sha256(v));
    }

    private static String base64(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static List<String> scopes(List<String> s) {
        return s == null || s.isEmpty() ? List.of("openid", "profile", "email") : s.stream().filter(x -> x.matches("[A-Za-z0-9._:-]+")).distinct().toList();
    }

    private static String status(String s) {
        if (!Set.of("active", "disabled").contains(s))
            throw new DomainException(ErrorCodeConstants.OIDC_STATUS_INVALID);
        return s;
    }

    private static ProviderView view(FederationProviderEntity p, String secret) {
        return new ProviderView(p.id(), p.tenantId(), p.name(), "oidc", p.issuer(), p.clientId(), secret, p.scopes(), p.claimMapping(), p.jitProvisioning(), p.status(), p.createdAt(), p.updatedAt());
    }

    @Transactional
    public ProviderView create(Input in) {
        return create(TenantContextHolder.requireTenantId(), in);
    }

    @Transactional(readOnly = true)
    public PageData<ProviderView> list(int page, int size, String search, String status) {
        return list(TenantContextHolder.requireTenantId(), page, size, search, status);
    }

    @Transactional(readOnly = true)
    public ProviderView get(UUID id) {
        return get(TenantContextHolder.requireTenantId(), id);
    }

    @Transactional
    public ProviderView update(UUID id, Input in) {
        return update(TenantContextHolder.requireTenantId(), id, in);
    }

    @Transactional
    public void delete(UUID id) {
        delete(TenantContextHolder.requireTenantId(), id);
    }

    public record Input(String name, String issuer, String clientId, String clientSecret, List<String> scopes,
                        Map<String, String> claimMapping, Boolean jitProvisioning, String status) {
    }

    public record ProviderView(UUID id, UUID tenantId, String name, String type, String issuer, String clientId,
                               String clientSecret, List<String> scopes, Map<String, String> claimMapping,
                               boolean jitProvisioning, String status, Instant createdAt, Instant updatedAt) {
    }

    public record AuthorizationStart(String authorizeUrl, String state, int expiresIn) {
    }

    public record LoginResult(UUID poolUserId, boolean isNewUser) {
    }
}
