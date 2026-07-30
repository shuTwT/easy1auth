package com.easy1auth.adminaccess.model;

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

@Entity
@Table(name = "admin_role")
public interface AdminRoleEntity {
    @Id
    UUID id();

    @Column(name = "tenant_id")
    UUID tenantId();

    String name();

    @Nullable
    String description();

    @ManyToMany
    @JoinTable(name = "admin_role_permission", joinColumnName = "role_id", inverseJoinColumnName = "permission_code")
    List<ManagementPermissionEntity> permissions();

    @Column(name = "system_role")
    boolean systemRole();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();

    @ManyToMany
    @JoinTable(name = "membership_role_assignment", joinColumnName = "role_id", inverseJoinColumnName = "membership_id")
    List<TenantMembershipEntity> memberships();
}
