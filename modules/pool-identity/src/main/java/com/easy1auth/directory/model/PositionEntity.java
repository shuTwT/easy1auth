package com.easy1auth.directory.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 岗位实体（对应 position 表）。
 *
 * <p>描述 pool_user 组织架构中的岗位，按租户隔离。岗位记录编码、层级与
 * 所属部门，用于组织架构与人员归属管理。</p>
 */
@Entity
@Table(name = "position")
public interface PositionEntity extends BaseEntity, BaseTenantEntity {
    /** 岗位名称（中文展示名） */
    String name();

    /** 岗位编码（租户内唯一） */
    String code();

    /** 岗位描述（可为 null） */
    @Nullable String description();

    /** 所属部门 ID（可为 null，表示不归属于具体部门） */
    @Column(name = "department_id")
    @Nullable UUID departmentId();

    /** 岗位层级（数值越大表示职级越高） */
    int level();

    /** 排序序号（可为 null） */
    @Nullable String sequence();

    /** 该岗位人数上限（可为 null，表示不限制） */
    @Column(name = "max_count")
    @Nullable Integer maxCount();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
