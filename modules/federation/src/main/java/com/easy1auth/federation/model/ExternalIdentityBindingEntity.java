package com.easy1auth.federation.model;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.*;
@Entity @Table(name="external_identity_binding") public interface ExternalIdentityBindingEntity {
 @Id UUID id(); @Column(name="tenant_id") UUID tenantId(); @Column(name="provider_id") UUID providerId(); @Column(name="pool_user_id") UUID poolUserId();
 String issuer(); String subject(); @Serialized Map<String,Object> claims(); @Column(name="created_at") Instant createdAt(); @Nullable @Column(name="last_login_at") Instant lastLoginAt();
}
