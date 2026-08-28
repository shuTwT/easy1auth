package com.easy1auth.useraccess.model;

import org.babyfish.jimmer.sql.*;

import java.util.UUID;

/**
 * 用户-角色分配联合主键（可嵌入对象）。
 *
 * <p>由租户 ID、用户 ID、角色 ID 三者共同唯一标识一条 pool_user 的角色分配，
 * 保证同一租户下用户与角色的分配关系唯一。</p>
 */
@Embeddable
public interface UserRoleAssignmentId {
    /** 租户 ID */
    @Column(name = "tenant_id")
    UUID tenantId();

    /** 用户 ID（pool_user） */
    @Column(name = "user_id")
    UUID userId();

    /** 角色 ID */
    @Column(name = "role_id")
    UUID roleId();
}
