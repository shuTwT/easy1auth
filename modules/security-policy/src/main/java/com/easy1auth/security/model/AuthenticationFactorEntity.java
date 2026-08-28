package com.easy1auth.security.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 认证因子实体（对应 authentication_factor 表）。
 *
 * <p>记录主体的多因素认证配置（当前支持 TOTP），密钥以 AES-GCM 加密存储；
 * 记录最后使用的时间步用于防重放。同主体同类型因子唯一。</p>
 */
@Entity
@Table(name = "authentication_factor")
public interface AuthenticationFactorEntity {
    /** 因子 ID */
    @Id
    UUID id();

    /** 主体类型（区分 admin_user / pool_user 等） */
    @Column(name = "subject_type")
    String subjectType();

    /** 主体 ID */
    @Column(name = "subject_id")
    UUID subjectId();

    /** 所属租户 ID（可为 null） */
    @Nullable
    @Column(name = "tenant_id")
    UUID tenantId();

    /** 因子类型（当前仅 totp） */
    @Column(name = "factor_type")
    String factorType();

    /** 因子密钥的密文（AES-GCM，明文不落库） */
    @Nullable
    @Column(name = "encrypted_secret")
    String encryptedSecret();

    /** 是否已启用（false 表示已设置但未完成激活） */
    boolean enabled();

    /** 最后成功使用的时间步（TOTP 防重放用，可为 null） */
    @Nullable
    @Column(name = "last_totp_step")
    Long lastTotpStep();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
