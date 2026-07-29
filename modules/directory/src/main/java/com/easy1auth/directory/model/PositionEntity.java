package com.easy1auth.directory.model;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="position") public interface PositionEntity{@Id UUID id();@Column(name="tenant_id")UUID tenantId();String name();String code();@Nullable String description();@Column(name="department_id")@Nullable UUID departmentId();int level();@Nullable String sequence();@Column(name="max_count")@Nullable Integer maxCount();@Column(name="created_at")Instant createdAt();@Column(name="updated_at")Instant updatedAt();}
