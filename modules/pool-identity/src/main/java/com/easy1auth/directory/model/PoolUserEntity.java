package com.easy1auth.directory.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "pool_user")
public interface PoolUserEntity extends BaseEntity, BaseTenantEntity {
    String username();

    String email();

    @Nullable String phone();

    @Column(name = "password_hash")
    @Nullable String passwordHash();

    String name();

    @Nullable String avatar();

    String status();

    @Column(name = "email_verified")
    boolean emailVerified();

    @Column(name = "phone_verified")
    boolean phoneVerified();

    @Nullable String department();

    @Nullable String position();

    @Column(name = "enterprise_identity_source_id")
    @Nullable UUID enterpriseIdentitySourceId();

    @Column(name = "enterprise_identity_external_id")
    @Nullable String enterpriseIdentityExternalId();

    @Serialized
    @Nullable Map<String, Object> customAttributes();

    @Column(name = "last_login_at")
    @Nullable Instant lastLoginAt();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
