package com.easy1auth.security.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 认证挑战实体（对应 authentication_challenge 表）。
 *
 * <p>记录一次待验证的认证挑战（邮箱验证码或 TOTP），令牌与验证码均以
 * SHA-256 哈希存储。消费时校验过期时间、尝试次数与重放，支持多因子与
 * 注册（subject_id 为空）等场景。</p>
 */
@Entity
@Table(name = "authentication_challenge")
public interface AuthenticationChallengeEntity {
    /** 挑战 ID */
    @Id
    UUID id();

    /** 挑战令牌的 SHA-256 哈希（明文令牌仅下发一次，不落库） */
    @Column(name = "token_hash")
    String tokenHash();

    /** 主体类型（如用户类型 / registration / 登录名等） */
    @Column(name = "subject_type")
    String subjectType();

    /** 主体 ID（注册场景用户尚未创建时为 null） */
    @Nullable
    @Column(name = "subject_id")
    UUID subjectId();

    /** 所属租户 ID（可为 null） */
    @Nullable
    @Column(name = "tenant_id")
    UUID tenantId();

    /** 挑战用途标识（区分场景，消费时须一致） */
    String purpose();

    /** 发送目的地（如邮箱地址，可为 null） */
    @Nullable
    String destination();

    /** 因子类型：email（邮箱验证码）/ totp（TOTP） */
    @Column(name = "factor_type")
    String factorType();

    /** 验证码的 SHA-256 哈希（TOTP 挑战为 null，实时比对） */
    @Nullable
    @Column(name = "code_hash")
    String codeHash();

    /** 已尝试次数（错误验证时递增） */
    int attempts();

    /** 允许的最大尝试次数 */
    @Column(name = "max_attempts")
    int maxAttempts();

    /** 过期时间（默认签发后 600 秒） */
    @Column(name = "expires_at")
    Instant expiresAt();

    /** 消费时间，非空表示已使用（防重放） */
    @Nullable
    @Column(name = "consumed_at")
    Instant consumedAt();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最近一次发送验证码的时间（用于限流判断，可为 null） */
    @Nullable
    @Column(name = "last_sent_at")
    Instant lastSentAt();
}
