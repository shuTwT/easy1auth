package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant")
public interface TenantEntity {
    @Id
    UUID id();

    String name();

    String status();

    @Column(name = "is_system")
    boolean isSystem();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "package_id")
    TenantPackageEntity packageInfo();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
