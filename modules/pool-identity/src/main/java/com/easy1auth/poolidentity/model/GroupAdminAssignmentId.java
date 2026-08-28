package com.easy1auth.poolidentity.model;

import org.babyfish.jimmer.sql.*;

import java.util.UUID;

/**
 * 用户-组管理员分配联合主键（可嵌入对象）。
 *
 * <p>由租户 ID、用户组 ID、用户 ID 三者共同唯一标识一条组管理员分配，
 * 保证同一租户下用户与用户组的管理员关系唯一。</p>
 */
@Embeddable
public interface GroupAdminAssignmentId {
    /** 租户 ID */
    @Column(name = "tenant_id")
    UUID tenantId();

    /** 用户组 ID */
    @Column(name = "group_id")
    UUID groupId();

    /** 用户 ID（pool_user，组管理员） */
    @Column(name = "user_id")
    UUID userId();
}
