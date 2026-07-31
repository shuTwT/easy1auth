package com.easy1auth.security.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "authentication_factor")
public interface AuthenticationFactorEntity {
    @Id
    UUID id();

    @Column(name = "subject_type")
    String subjectType();

    @Column(name = "subject_id")
    UUID subjectId();

    @Nullable
    @Column(name = "tenant_id")
    UUID tenantId();

    @Column(name = "factor_type")
    String factorType();

    @Nullable
    @Column(name = "encrypted_secret")
    String encryptedSecret();

    boolean enabled();

    @Nullable
    @Column(name = "last_totp_step")
    Long lastTotpStep();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
