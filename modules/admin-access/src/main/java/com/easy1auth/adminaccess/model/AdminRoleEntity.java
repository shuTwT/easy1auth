package com.easy1auth.adminaccess.model;
import com.easy1auth.tenant.model.TenantMembershipEntity; import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.*;
@Entity @Table(name="admin_role") public interface AdminRoleEntity{
 @Id UUID id(); @Column(name="tenant_id") UUID tenantId(); String name(); @Nullable String description(); @Serialized List<String> permissions(); @Column(name="system_role") boolean systemRole(); @Column(name="created_at") Instant createdAt(); @Column(name="updated_at") Instant updatedAt();
 @ManyToMany @JoinTable(name="membership_role_assignment",joinColumnName="role_id",inverseJoinColumnName="membership_id") List<TenantMembershipEntity> memberships();
}
