package com.easy1auth.application.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="oauth_application")
public interface OAuthApplicationEntity {
    @Id UUID id();
    @Column(name="tenant_id") UUID tenantId();
    String name();
    @Nullable String logo();
    @Nullable String description();
    String type();
    @Column(name="client_id") String clientId();
    @Column(name="client_secret_hash") @Nullable String clientSecretHash();
    @Serialized @Column(name="redirect_uris") List<String> redirectUris();
    @Serialized @Column(name="post_logout_redirect_uris") List<String> postLogoutRedirectUris();
    @Serialized @Column(name="allowed_grant_types") List<String> allowedGrantTypes();
    @Serialized List<String> scopes();
    @Column(name="require_pkce") boolean requirePkce();
    @Column(name="require_consent") boolean requireConsent();
    @Column(name="access_token_lifetime") int accessTokenLifetime();
    @Column(name="refresh_token_lifetime") int refreshTokenLifetime();
    String status();
    @Column(name="created_at") Instant createdAt();
    @Column(name="updated_at") Instant updatedAt();
}
