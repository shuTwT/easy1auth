package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 租户实体（对应 tenant 表）。
 *
 * <p>租户是平台多租户体系的核心概念，分为系统租户（isSystem=true，平台自身使用）
 * 与普通租户（isSystem=false，面向客户运营）。普通租户绑定一个租户套餐
 * （{@link TenantPackageEntity}），用于配额与权限控制。</p>
 */
@Entity
@Table(name = "tenant")
public interface TenantEntity {
    /** 租户 ID */
    @Id
    UUID id();

    /** 租户名称 */
    String name();

    /** 租户状态：active（正常）/ suspended（停用）/ deleted（已删除） */
    String status();

    /** 是否系统租户：true 表示平台内置租户，false 表示普通租户 */
    @Column(name = "is_system")
    boolean isSystem();

    /** 绑定的租户套餐（多对一，普通租户必须绑定） */
    @Nullable
    @ManyToOne
    @JoinColumn(name = "package_id")
    TenantPackageEntity packageInfo();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
