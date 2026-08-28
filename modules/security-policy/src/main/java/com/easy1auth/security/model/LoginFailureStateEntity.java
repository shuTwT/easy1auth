package com.easy1auth.security.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 登录失败状态实体（对应 login_failure_state 表）。
 *
 * <p>以「主体类型 + 主体键 + 租户」为维度记录连续登录失败次数与锁定时间，
 * 供登录保护服务判断是否触发临时锁定。登录成功后记录会被清除。</p>
 */
@Entity
@Table(name = "login_failure_state")
public interface LoginFailureStateEntity {
    /** 记录 ID */
    @Id
    UUID id();

    /** 主体类型（登录名 / 设备 / 验证码等场景标识） */
    @Column(name = "subject_type")
    String subjectType();

    /** 主体键（归一化后的登录名等，超长截断，不区分大小写） */
    @Column(name = "subject_key")
    String subjectKey();

    /** 所属租户 ID（可为 null，如平台级主体） */
    @Nullable
    @Column(name = "tenant_id")
    UUID tenantId();

    /** 当前连续失败次数 */
    @Column(name = "failed_attempts")
    int failedAttempts();

    /** 锁定截止时间，非空且未过时表示处于锁定中 */
    @Nullable
    @Column(name = "locked_until")
    Instant lockedUntil();

    /** 最近一次失败时间（可为 null） */
    @Nullable
    @Column(name = "last_failed_at")
    Instant lastFailedAt();
}
