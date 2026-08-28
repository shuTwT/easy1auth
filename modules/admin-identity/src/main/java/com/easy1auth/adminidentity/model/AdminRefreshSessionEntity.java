package com.easy1auth.adminidentity.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 管理后台刷新会话实体（对应 admin_refresh_session 表）。
 *
 * <p>记录管理账号（admin_user）签发的刷新令牌摘要及其安全版本、有效期等，
 * 用于刷新令牌的校验、轮换与吊销。令牌原文不落库，仅保存其 SHA-256 哈希。</p>
 */
@Entity
@Table(name = "admin_refresh_session")
public interface AdminRefreshSessionEntity {
    /** 会话 ID */
    @Id
    UUID id();

    /** 所属管理账号 ID */
    @Column(name = "account_id")
    UUID accountId();

    /** 刷新令牌的 SHA-256 哈希（令牌原文不存储） */
    @Column(name = "token_hash")
    String tokenHash();

    /** 签发时的账号安全版本号，用于检测会话是否因密码/邮箱等变更而失效 */
    @Column(name = "security_version")
    long securityVersion();

    /** 会话过期时间 */
    @Column(name = "expires_at")
    Instant expiresAt();

    /** 吊销时间，非空表示该会话已被吊销 */
    @Column(name = "revoked_at")
    @Nullable Instant revokedAt();

    /** 轮换后新会话的 ID（关联被替换的旧会话） */
    @Column(name = "replaced_by")
    @Nullable UUID replacedBy();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最近一次使用（刷新）时间 */
    @Column(name = "last_used_at")
    @Nullable Instant lastUsedAt();

    /** 签发时的用户代理（UA），用于审计 */
    @Column(name = "user_agent")
    @Nullable String userAgent();

    /** 签发时的来源 IP，用于审计 */
    @Column(name = "ip_address")
    @Nullable String ipAddress();
}
