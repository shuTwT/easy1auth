package com.easy1auth.federation.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "external_identity_binding")
public interface ExternalIdentityBindingEntity extends BaseEntity, BaseTenantEntity {
    @Column(name = "provider_id")
    UUID providerId();

    @Column(name = "pool_user_id")
    UUID poolUserId();

    String issuer();

    String subject();

    @Serialized
    Map<String, Object> claims();

    @Column(name = "created_at")
    Instant createdAt();

    @Nullable
    @Column(name = "last_login_at")
    Instant lastLoginAt();
}
