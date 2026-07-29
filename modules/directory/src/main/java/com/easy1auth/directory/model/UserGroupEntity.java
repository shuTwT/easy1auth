package com.easy1auth.directory.model;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="user_group") public interface UserGroupEntity{@Id UUID id();@Column(name="tenant_id")UUID tenantId();String name();@Nullable String description();String type();@Column(name="parent_id")@Nullable UUID parentId();@Column(name="created_at")Instant createdAt();@Column(name="updated_at")Instant updatedAt();}
