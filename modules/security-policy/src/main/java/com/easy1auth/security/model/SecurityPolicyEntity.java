package com.easy1auth.security.model;

import com.easy1auth.infrastructure.persistence.model.BaseEntity;
import com.easy1auth.infrastructure.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;

import java.time.Instant;

/**
 * 安全策略实体（对应 security_policy 表）。
 *
 * <p>保存租户（tenant_id 为空的记录为平台全局策略）的密码强度、有效期、
 * 防重用、MFA 要求与登录锁定等配置，供登录与改密流程强制执行。</p>
 */
@Entity
@Table(name = "security_policy")
public interface SecurityPolicyEntity extends BaseEntity, BaseTenantEntity {
    /** 密码最小长度（默认 8，合法范围 8-128） */
    @Column(name = "password_min_length")
    int passwordMinLength();

    /** 是否要求密码含大写字母 */
    @Column(name = "password_require_upper")
    boolean passwordRequireUpper();

    /** 是否要求密码含小写字母 */
    @Column(name = "password_require_lower")
    boolean passwordRequireLower();

    /** 是否要求密码含数字 */
    @Column(name = "password_require_number")
    boolean passwordRequireNumber();

    /** 是否要求密码含特殊字符 */
    @Column(name = "password_require_special")
    boolean passwordRequireSpecial();

    /** 密码最长有效期（天），过期后强制改密 */
    @Column(name = "password_max_age_days")
    int passwordMaxAgeDays();

    /** 防重用保留的历史密码条数（0-24） */
    @Column(name = "password_history_count")
    int passwordHistoryCount();

    /** 是否强制要求启用 MFA */
    @Column(name = "mfa_required")
    boolean mfaRequired();

    /** 连续登录失败锁定阈值 */
    @Column(name = "login_attempt_limit")
    int loginAttemptLimit();

    /** 锁定持续时间（秒） */
    @Column(name = "lockout_duration_seconds")
    int lockoutDurationSeconds();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
