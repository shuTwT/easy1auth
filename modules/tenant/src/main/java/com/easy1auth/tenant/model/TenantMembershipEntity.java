package com.easy1auth.tenant.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;

import org.babyfish.jimmer.sql.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_membership")
public interface TenantMembershipEntity extends BaseEntity, BaseTenantEntity {
    @Column(name = "account_id") UUID accountId();
    @Column(name = "membership_role") String membershipRole();
    String status();
    @Column(name = "created_at") Instant createdAt();
    @Column(name = "updated_at") Instant updatedAt();
}
