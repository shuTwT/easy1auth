package com.easy1auth.security.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "authentication_recovery_code")
public interface AuthenticationRecoveryCodeEntity {
    @Id
    UUID id();

    @Column(name = "factor_id")
    UUID factorId();

    @Column(name = "code_hash")
    String codeHash();

    @Nullable
    @Column(name = "used_at")
    Instant usedAt();

    @Column(name = "created_at")
    Instant createdAt();
}
