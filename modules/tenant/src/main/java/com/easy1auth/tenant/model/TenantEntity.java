package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant")
public interface TenantEntity {
    @Id UUID id();
    String name();
    String status();
    String plan();
    @Column(name = "max_users") int maxUsers();
    @Column(name = "max_apps") int maxApps();
    @Column(name = "created_at") Instant createdAt();
    @Column(name = "updated_at") Instant updatedAt();
}
