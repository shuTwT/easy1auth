package com.easy1auth.useraccess.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pool_permission")
public interface PoolPermissionEntity extends BaseEntity, BaseTenantEntity {
    String code();

    String name();

    @Nullable String description();

    String type();

    @Column(name = "parent_id")
    @Nullable UUID parentId();

    String resource();

    String action();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
