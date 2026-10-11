package com.easy1auth.poolidentity.model;

import org.babyfish.jimmer.sql.*;

import java.util.UUID;

/**
 * 用户-组成员分配联合主键（可嵌入对象）。
 *
 * <p>由用户池 ID、用户 ID、用户组 ID 三者共同唯一标识一条组成员分配，
 * 保证同一用户池下用户与用户组的从属关系唯一。</p>
 */
@Embeddable
public interface UserGroupAssignmentId {
    /** 用户池 ID */
    @Column(name = "user_pool_id")
    UUID userPoolId();

    /** 用户 ID（pool_user，组成员） */
    @Column(name = "user_id")
    UUID userId();

    /** 用户组 ID */
    @Column(name = "group_id")
    UUID groupId();
}
