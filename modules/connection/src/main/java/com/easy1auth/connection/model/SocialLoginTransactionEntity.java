package com.easy1auth.connection.model;

import com.easy1auth.common.persistence.model.BaseEntity;
import com.easy1auth.common.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 社交登录事务：记录 authorize 阶段生成的 state/nonce，用于回调时防重放。
 *
 * <p>发起授权时落库一条事务，回调时按 state 哈希定位并校验有效期与
 * return_uri 一致性，消费后标记 consumed_at 防止重放。</p>
 */
@Entity
@Table(name = "social_login_transaction")
public interface SocialLoginTransactionEntity extends BaseEntity, BaseTenantEntity {
    /** 身份源 ID（social_identity_source） */
    @Column(name = "source_id")
    UUID sourceId();

    /** 授权 state 的 SHA-256 哈希（明文 state 不落库） */
    @Column(name = "state_hash")
    String stateHash();

    /** nonce 的 SHA-256 哈希 */
    @Column(name = "nonce_hash")
    String nonceHash();

    /** nonce 的加密密文（AES-GCM） */
    @Column(name = "encrypted_nonce")
    String encryptedNonce();

    /** PKCE code_verifier 的加密密文（不支持 PKCE 的厂商为 null） */
    @Nullable
    @Column(name = "encrypted_pkce_verifier")
    String encryptedPkceVerifier();

    /** 回调校验用的 return_uri（校验与授权时一致） */
    @Nullable
    @Column(name = "return_uri")
    String returnUri();

    /** 事务过期时间（默认签发后 600 秒） */
    @Column(name = "expires_at")
    Instant expiresAt();

    /** 消费时间，非空表示已使用（防重放） */
    @Nullable
    @Column(name = "consumed_at")
    Instant consumedAt();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();
}
