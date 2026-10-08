package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户池实体（对应 tenant 表）。
 *
 * <p>用户池是平台多用户池体系的核心概念，分为系统用户池（isSystem=true，平台自身使用）
 * 与普通用户池（isSystem=false，面向客户运营）。普通用户池绑定一个用户池套餐
 * （{@link UserPoolPackageEntity}），用于配额与权限控制。</p>
 */
@Entity
@Table(name = "tenant")
public interface UserPoolEntity {
    /** 用户池 ID */
    @Id
    UUID id();

    /** 用户池名称 */
    String name();

    /** 用户池状态：active（正常）/ suspended（停用）/ deleted（已删除） */
    String status();

    /** 是否系统用户池：true 表示平台内置用户池，false 表示普通用户池 */
    @Column(name = "is_system")
    boolean isSystem();

    /** 绑定的用户池套餐（多对一，普通用户池必须绑定） */
    @Nullable
    @ManyToOne
    @JoinColumn(name = "package_id")
    UserPoolPackageEntity packageInfo();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
