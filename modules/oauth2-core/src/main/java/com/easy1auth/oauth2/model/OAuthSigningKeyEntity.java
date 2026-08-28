package com.easy1auth.oauth2.model;

import com.easy1auth.infrastructure.persistence.model.BaseEntity;
import com.easy1auth.infrastructure.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * OAuth2 签名密钥实体（对应 oauth2_signing_key 表）。
 *
 * <p>保存每个租户用于 OIDC/JWT 签名的 RSA 密钥：公钥以 JSON 明文存储，
 * 私钥以 AES-GCM 加密后的密文存储。同一租户同一时间只有一把激活密钥。</p>
 */
@Entity
@Table(name = "oauth2_signing_key")
public interface OAuthSigningKeyEntity extends BaseEntity, BaseTenantEntity {
    /** 密钥 ID（即 JWK 的 kid 字段，用于标识当前使用的签名密钥） */
    @Column(name = "key_id")
    String keyId();

    /** 签名算法：当前固定为 RS256 */
    String algorithm();

    /** 公钥 JWK（序列化后的 JSON Map，可对外公开，供 JWT 验签） */
    @Serialized
    @Column(name = "public_jwk")
    Map<String, Object> publicJwk();

    /** 私钥 JWK 的加密密文（AES-GCM 加密后 Base64 编码，不可对外泄露） */
    @Column(name = "encrypted_private_jwk")
    String encryptedPrivateJwk();

    /** 密钥状态：active（激活，当前用于签名） */
    String status();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 过期时间（可为 null，表示永不过期） */
    @Column(name = "expires_at")
    @Nullable Instant expiresAt();
}
