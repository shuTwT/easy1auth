package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_membership")
public interface TenantMembershipEntity {
    @Id UUID id();
    @Column(name = "account_id") UUID accountId();
    @Column(name = "tenant_id") UUID tenantId();
    @Column(name = "membership_role") String membershipRole();
    String status();
    @Column(name = "created_at") Instant createdAt();
    @Column(name = "updated_at") Instant updatedAt();
}
