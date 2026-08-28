package com.easy1auth.application.repository;

import com.easy1auth.application.model.OAuthApplicationEntity;
import com.easy1auth.application.model.OAuthApplicationEntityTable;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth 应用数据仓储。
 */
public interface ApplicationRepository extends JRepository<OAuthApplicationEntity, UUID> {
    OAuthApplicationEntityTable APP = OAuthApplicationEntityTable.$;

    default long count() {
        return sql().createQuery(APP).select(APP.id()).fetchUnlimitedCount();
    }

    default boolean nameExists(String name, UUID excluding) {
        return sql().createQuery(APP).where(APP.name().eq(name)).whereIf(excluding != null, () -> APP.id().ne(excluding)).select(APP.id()).exists();
    }

    default List<OAuthApplicationEntity> list(String name, String type, String status) {
        return sql().createQuery(APP).whereIf(name != null && !name.isBlank(), () -> APP.name().ilike(name, LikeMode.ANYWHERE)).whereIf(type != null && !type.isBlank(), () -> APP.type().eq(type)).whereIf(status != null && !status.isBlank(), () -> APP.status().eq(status)).orderBy(APP.createdAt().desc()).select(APP).execute();
    }

    default Optional<OAuthApplicationEntity> find(UUID id) {
        return sql().createQuery(APP).where(APP.id().eq(id)).select(APP).fetchOptional();
    }

    default Optional<OAuthApplicationEntity> findActiveByClientId(String clientId) {
        return sql().createQuery(APP).where(APP.clientId().eq(clientId), APP.status().eq("active")).select(APP).fetchOptional();
    }

    default Optional<OAuthApplicationEntity> findActive(UUID tenant, UUID id) {
        return sql().createQuery(APP).where(APP.id().eq(id), APP.tenantId().eq(tenant), APP.status().eq("active")).select(APP).fetchOptional();
    }

    default void saveApplication(OAuthApplicationEntity app) {
        sql().saveCommand(app).setMode(SaveMode.INSERT_ONLY).execute();
    }

    default int update(UUID tenant, UUID id, String name, String logo, String description, String type, List<String> redirects, List<String> logout, List<String> grants, List<String> scopes, boolean pkce, boolean consent, int access, int refresh, String secretHash) {
        var u = sql().createUpdate(APP).set(APP.name(), name).set(APP.logo(), logo).set(APP.description(), description).set(APP.type(), type).set(APP.redirectUris(), redirects).set(APP.postLogoutRedirectUris(), logout).set(APP.allowedGrantTypes(), grants).set(APP.scopes(), scopes).set(APP.requirePkce(), pkce).set(APP.requireConsent(), consent).set(APP.accessTokenLifetime(), access).set(APP.refreshTokenLifetime(), refresh).set(APP.updatedAt(), Instant.now()).where(APP.id().eq(id), APP.tenantId().eq(tenant));
        if (secretHash != null) u.set(APP.clientSecretHash(), secretHash);
        return u.execute();
    }

    default int updateStatus(UUID tenant, UUID id, String status) {
        return sql().createUpdate(APP).set(APP.status(), status).set(APP.updatedAt(), Instant.now()).where(APP.id().eq(id), APP.tenantId().eq(tenant)).execute();
    }

    default int updateSecret(UUID tenant, UUID id, String hash) {
        return sql().createUpdate(APP).set(APP.clientSecretHash(), hash).set(APP.updatedAt(), Instant.now()).where(APP.id().eq(id), APP.tenantId().eq(tenant)).execute();
    }

    default int delete(UUID tenant, UUID id) {
        return sql().createDelete(APP).where(APP.id().eq(id), APP.tenantId().eq(tenant)).execute();
    }

    default List<String> statuses() {
        return sql().createQuery(APP).select(APP.status()).execute();
    }
}
