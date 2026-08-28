package com.easy1auth.directory.model;

import org.babyfish.jimmer.sql.*;

/**
 * 用户-组成员分配实体（对应 pool_user_group 表）。
 *
 * <p>记录 pool_user 与用户组之间的多对多从属关系，使用内嵌联合主键
 * {@link UserGroupAssignmentId}（租户 ID + 用户 ID + 用户组 ID）唯一标识一条分配。</p>
 */
@Entity
@Table(name = "pool_user_group")
public interface UserGroupAssignmentEntity {
    /** 分配关系联合主键（tenantId + userId + groupId） */
    @Id
    UserGroupAssignmentId id();
}
