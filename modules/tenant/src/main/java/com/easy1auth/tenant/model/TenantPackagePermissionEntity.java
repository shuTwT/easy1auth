package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.time.Instant;

@Entity
@Table(name = "tenant_package_permission")
public interface TenantPackagePermissionEntity {
    @Id
    TenantPackagePermissionId id();

    @Column(name = "created_at")
    Instant createdAt();
}
