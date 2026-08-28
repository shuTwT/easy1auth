package com.easy1auth.useraccess.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * 角色实体（对应 pool_role 表）。
 *
 * <p>描述 pool_user 访问控制中的角色，按租户隔离。角色分两类：
 * system（内置角色，仅系统初始化、不可修改删除）/ custom（自定义角色）。
 * 角色可配置数据范围（dataScope）与权限集合（permissions），
 * 并可通过 {@code parentId} 组成层级。</p>
 */
@Entity
@Table(name = "pool_role")
public interface PoolRoleEntity extends BaseEntity, BaseTenantEntity {
    /** 角色名称（中文展示名） */
    String name();

    /** 角色编码（租户内唯一，用于程序识别） */
    String code();

    /** 角色描述（可为 null） */
    @Nullable String description();

    /** 角色类型：system（内置）/ custom（自定义） */
    String type();

    /** 权限集合（权限码 -> 是否启用），以序列化 JSON 存储 */
    @Serialized
    Map<String, Boolean> permissions();

    /** 数据范围：all（全部数据）/ department（本部门）/ department_and_sub（本部门及下级）/ self（仅本人） */
    @Column(name = "data_scope")
    String dataScope();

    /** 父角色 ID（可为 null，表示顶级角色） */
    @Column(name = "parent_id")
    @Nullable UUID parentId();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
