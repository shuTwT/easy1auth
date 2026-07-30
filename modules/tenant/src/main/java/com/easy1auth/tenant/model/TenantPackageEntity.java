package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.time.Instant;

@Entity
@Table(name = "tenant_package")
public interface TenantPackageEntity {
    @Id
    long id();

    String code();

    String name();

    String status();

    @Column(name = "is_default")
    boolean defaultPackage();

    @Column(name = "max_users")
    int maxUsers();

    @Column(name = "max_apps")
    int maxApps();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
