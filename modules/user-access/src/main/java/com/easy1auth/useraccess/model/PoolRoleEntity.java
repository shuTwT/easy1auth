package com.easy1auth.useraccess.model;
import com.easy1auth.persistence.model.BaseEntity;import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;import org.jspecify.annotations.Nullable;import java.time.Instant;import java.util.*;
@Entity @Table(name="pool_role")public interface PoolRoleEntity extends BaseEntity, BaseTenantEntity {String name();String code();@Nullable String description();String type();@Serialized Map<String,Boolean> permissions();@Column(name="data_scope")String dataScope();@Column(name="parent_id")@Nullable UUID parentId();@Column(name="created_at")Instant createdAt();@Column(name="updated_at")Instant updatedAt();}
