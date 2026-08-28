package com.easy1auth.adminidentity.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 管理后台账号实体（对应 admin_account 表）。
 *
 * <p>表示平台管理端（admin_user）的登录账号，区别于面向第三方接入的 pool_user。
 * 一个管理员可拥有多个租户，账号本身与租户解耦。</p>
 */
@Entity
@Table(name = "admin_account")
public interface AdminAccountEntity {
    /** 账号 ID */
    @Id
    UUID id();

    /** 用户名（唯一，登录凭证之一） */
    String username();

    /** 邮箱（唯一，登录凭证之一） */
    String email();

    /** 手机号（可为空） */
    @Nullable String phone();

    /** 账号状态：active（正常）/ disabled（禁用） */
    String status();

    /** 最近一次切换使用的租户 ID */
    @Column(name = "last_tenant_id")
    @Nullable UUID lastTenantId();

    /** 安全版本号：密码/邮箱/MFA/状态等敏感信息变更时递增，用于使旧会话失效 */
    @Column(name = "security_version")
    long securityVersion();

    /** 是否已启用多因素认证（MFA） */
    @Column(name = "mfa_enabled")
    boolean mfaEnabled();

    /** MFA 类型（如 totp），未启用时为空 */
    @Column(name = "mfa_type")
    @Nullable String mfaType();

    /** 最近一次登录时间 */
    @Column(name = "last_login_at")
    @Nullable Instant lastLoginAt();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
