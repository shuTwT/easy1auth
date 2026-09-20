package com.easy1auth.poolidentity.model;

import com.easy1auth.framework.persistence.model.BaseEntity;
import com.easy1auth.framework.tenant.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "pool_permission_space")
public interface PoolPermissionSpaceEntity extends BaseEntity, BaseTenantEntity {
    String name();
    String code();
    @Nullable String description();
    @Column(name = "created_at") Instant createdAt();
    @Column(name = "updated_at") Instant updatedAt();
}
