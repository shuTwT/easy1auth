package com.easy1auth.useraccess.model;

import org.babyfish.jimmer.sql.*;

/**
 * 用户-角色分配实体（对应 pool_user_role 表）。
 *
 * <p>记录 pool_user 与角色之间的多对多分配关系，使用内嵌联合主键
 * {@link UserRoleAssignmentId}（租户 ID + 用户 ID + 角色 ID）唯一标识一条分配。</p>
 */
@Entity
@Table(name = "pool_user_role")
public interface UserRoleAssignmentEntity {
    /** 分配关系联合主键（tenantId + userId + roleId） */
    @Id
    UserRoleAssignmentId id();
}
