package com.easy1auth.adminidentity.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_refresh_session")
public interface AdminRefreshSessionEntity {
    @Id
    UUID id();

    @Column(name = "account_id")
    UUID accountId();

    @Column(name = "token_hash")
    String tokenHash();

    @Column(name = "security_version")
    long securityVersion();

    @Column(name = "expires_at")
    Instant expiresAt();

    @Column(name = "revoked_at")
    @Nullable Instant revokedAt();

    @Column(name = "replaced_by")
    @Nullable UUID replacedBy();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "last_used_at")
    @Nullable Instant lastUsedAt();

    @Column(name = "user_agent")
    @Nullable String userAgent();

    @Column(name = "ip_address")
    @Nullable String ipAddress();
}
