package com.easy1auth.useraccess.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 权限实体（对应 pool_permission 表）。
 *
 * <p>描述 pool_user 访问控制中的权限点，按租户隔离。权限分三类：
 * menu（菜单）/ operation（操作）/ data（数据），可组成树形结构
 * （通过 {@code parentId} 关联父权限）。</p>
 */
@Entity
@Table(name = "pool_permission")
public interface PoolPermissionEntity extends BaseEntity, BaseTenantEntity {
    /** 权限编码（如 user:read，租户内唯一） */
    String code();

    /** 权限名称（中文展示名） */
    String name();

    /** 权限描述（可为 null） */
    @Nullable String description();

    /** 权限类型：menu（菜单）/ operation（操作）/ data（数据） */
    String type();

    /** 父权限 ID（可为 null，表示顶级权限） */
    @Column(name = "parent_id")
    @Nullable UUID parentId();

    /** 权限对应的资源标识（如 user / group / role） */
    String resource();

    /** 权限对应的动作（如 read / create / update / delete / assign） */
    String action();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
