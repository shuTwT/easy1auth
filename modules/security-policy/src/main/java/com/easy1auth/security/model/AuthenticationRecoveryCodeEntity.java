package com.easy1auth.security.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * TOTP 一次性备用码实体（对应 authentication_recovery_code 表）。
 *
 * <p>TOTP 设置时签发 10 个备用码，供用户丢失认证器时应急登录；
 * 备用码以 SHA-256 哈希存储，使用一次后标记 used_at。</p>
 */
@Entity
@Table(name = "authentication_recovery_code")
public interface AuthenticationRecoveryCodeEntity {
    /** 备用码 ID */
    @Id
    UUID id();

    /** 所属认证因子 ID（authentication_factor） */
    @Column(name = "factor_id")
    UUID factorId();

    /** 备用码的 SHA-256 哈希（明文仅设置时下发一次） */
    @Column(name = "code_hash")
    String codeHash();

    /** 使用时间，非空表示已被消费 */
    @Nullable
    @Column(name = "used_at")
    Instant usedAt();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();
}
