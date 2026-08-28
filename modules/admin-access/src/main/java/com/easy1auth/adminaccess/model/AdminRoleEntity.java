package com.easy1auth.adminaccess.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import com.easy1auth.tenant.model.TenantMembershipEntity;
import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.JoinTable;
import org.babyfish.jimmer.sql.ManyToMany;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 管理角色实体（对应 admin_role 表）。
 *
 * <p>描述租户内用于管理端授权的角色，可关联多个管理端权限
 * （{@link ManagementPermissionEntity}，经 admin_role_permission 关联表），
 * 并通过 membership_role_assignment 关联表与租户成员关系
 * （{@link TenantMembershipEntity}）多对多关联，即角色直接赋予成员。</p>
 */
@Entity
@Table(name = "admin_role")
public interface AdminRoleEntity extends BaseEntity, BaseTenantEntity {

    /** 角色名称 */
    String name();

    /** 角色描述（可为 null） */
    @Nullable
    String description();

    /** 角色关联的管理端权限列表（多对多） */
    @ManyToMany
    @JoinTable(name = "admin_role_permission", joinColumnName = "role_id", inverseJoinColumnName = "permission_code")
    List<ManagementPermissionEntity> permissions();

    /** 是否系统预置角色：true 表示由平台预置，不可修改或删除 */
    @Column(name = "system_role")
    boolean systemRole();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();

    /** 被分配该角色的租户成员关系列表（多对多） */
    @ManyToMany
    @JoinTable(name = "membership_role_assignment", joinColumnName = "role_id", inverseJoinColumnName = "membership_id")
    List<TenantMembershipEntity> memberships();
}
