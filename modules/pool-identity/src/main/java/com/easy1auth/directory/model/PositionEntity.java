package com.easy1auth.directory.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "position")
public interface PositionEntity extends BaseEntity, BaseTenantEntity {
    String name();

    String code();

    @Nullable String description();

    @Column(name = "department_id")
    @Nullable UUID departmentId();

    int level();

    @Nullable String sequence();

    @Column(name = "max_count")
    @Nullable Integer maxCount();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
