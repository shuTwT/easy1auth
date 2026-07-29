package com.easy1auth.directory.model;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.*;
@Entity @Table(name="pool_user") public interface PoolUserEntity{
 @Id UUID id(); @Column(name="tenant_id") UUID tenantId(); String username(); String email(); @Nullable String phone(); @Column(name="password_hash") @Nullable String passwordHash(); String name(); @Nullable String avatar(); String status(); @Column(name="email_verified") boolean emailVerified(); @Column(name="phone_verified") boolean phoneVerified(); @Nullable String department(); @Nullable String position(); @Serialized @Nullable Map<String,Object> customAttributes(); @Column(name="last_login_at") @Nullable Instant lastLoginAt(); @Column(name="created_at") Instant createdAt(); @Column(name="updated_at") Instant updatedAt();
}
