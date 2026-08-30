package com.easy1auth.adminaccess.model;

import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

/**
 * 管理端权限实体（对应 management_permission 表）。
 *
 * <p>管理端权限目录的数据源，记录所有可授权的权限码及其元数据。权限码的
 * 类型、作用域由枚举（{@link ManagementPermissionCode}）
 * 承载并在解析时校验，type 字段保存对应的数据库值。active 控制该权限是否
 * 可被授予。</p>
 */
@Entity
@Table(name = "management_permission")
public interface ManagementPermissionEntity {
    /** 权限码（如 "user:list"，全局唯一主键） */
    @Id
    String code();

    /** 权限类型数据库值：menu / directory / action */
    String type();

    /** 权限显示名称 */
    String name();

    /** 父级权限码（可为 null，用于构建树形目录） */
    @Nullable
    @Column(name = "parent_code")
    String parentCode();

    /** 权限所属资源 */
    String resource();

    /** 权限动作（如 list / create） */
    String action();

    /** 排序序号（升序展示） */
    @Column(name = "sort_order")
    int sortOrder();

    /** 是否启用 */
    boolean active();
}
