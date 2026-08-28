package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.time.Instant;

/**
 * 租户套餐实体（对应 tenant_package 表）。
 *
 * <p>描述普通租户绑定的套餐，决定其用户数、应用数配额与可授予的权限。
 * 注意：ID 为 0 的“系统套餐”不落库，由服务层在代码中构造（见 TenantPackageService）。</p>
 */
@Entity
@Table(name = "tenant_package")
public interface TenantPackageEntity {
    /** 套餐 ID（0 表示内置系统套餐，不落库） */
    @Id
    long id();

    /** 套餐编码（全局唯一） */
    String code();

    /** 套餐名称 */
    String name();

    /** 套餐状态：active（启用）/ inactive（停用） */
    String status();

    /** 是否为默认套餐（新建普通租户默认绑定） */
    @Column(name = "is_default")
    boolean defaultPackage();

    /** 允许的最大用户数 */
    @Column(name = "max_users")
    int maxUsers();

    /** 允许的最大应用数 */
    @Column(name = "max_apps")
    int maxApps();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
