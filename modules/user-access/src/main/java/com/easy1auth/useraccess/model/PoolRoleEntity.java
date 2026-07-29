package com.easy1auth.useraccess.model;
import org.babyfish.jimmer.sql.*;import org.jspecify.annotations.Nullable;import java.time.Instant;import java.util.*;
@Entity @Table(name="pool_role")public interface PoolRoleEntity{@Id UUID id();@Column(name="tenant_id")UUID tenantId();String name();String code();@Nullable String description();String type();@Serialized Map<String,Boolean> permissions();@Column(name="data_scope")String dataScope();@Column(name="parent_id")@Nullable UUID parentId();@Column(name="created_at")Instant createdAt();@Column(name="updated_at")Instant updatedAt();}
