package com.easy1auth.poolidentity.model;

import org.babyfish.jimmer.sql.*;

/**
 * 用户-组管理员分配实体（对应 pool_group_admin 表）。
 *
 * <p>记录 pool_user 作为用户组管理员的关系，使用内嵌联合主键
 * {@link GroupAdminAssignmentId}（租户 ID + 用户组 ID + 用户 ID）唯一标识一条分配。</p>
 */
@Entity
@Table(name = "pool_group_admin")
public interface GroupAdminAssignmentEntity {
    /** 分配关系联合主键（tenantId + groupId + userId） */
    @Id
    GroupAdminAssignmentId id();
}
