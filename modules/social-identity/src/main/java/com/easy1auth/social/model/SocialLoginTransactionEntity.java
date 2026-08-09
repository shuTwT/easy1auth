package com.easy1auth.social.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 社交登录事务：记录 authorize 阶段生成的 state/nonce，用于回调时防重放。
 */
@Entity
@Table(name = "social_login_transaction")
public interface SocialLoginTransactionEntity extends BaseEntity, BaseTenantEntity {
    @Column(name = "source_id")
    UUID sourceId();

    @Column(name = "state_hash")
    String stateHash();

    @Column(name = "nonce_hash")
    String nonceHash();

    @Column(name = "encrypted_nonce")
    String encryptedNonce();

    @Nullable
    @Column(name = "encrypted_pkce_verifier")
    String encryptedPkceVerifier();

    @Nullable
    @Column(name = "return_uri")
    String returnUri();

    @Column(name = "expires_at")
    Instant expiresAt();

    @Nullable
    @Column(name = "consumed_at")
    Instant consumedAt();

    @Column(name = "created_at")
    Instant createdAt();
}
