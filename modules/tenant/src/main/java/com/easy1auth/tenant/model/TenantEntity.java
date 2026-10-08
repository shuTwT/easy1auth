package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/** 用户池下的业务租户。仅维护基础资料，不参与管理端认证、授权或资源隔离。 */
@Entity
@Table(name = "pool_tenant")
public interface TenantEntity {
    @Id
    UUID id();

    @Column(name = "user_pool_id")
    UUID userPoolId();

    String name();

    String code();

    @Nullable
    String description();

    String status();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
