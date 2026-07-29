package com.easy1auth.adminidentity.model;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="admin_account") public interface AdminAccountEntity{
 @Id UUID id(); String username(); String email(); @Nullable String phone(); String status();
 @Column(name="last_tenant_id") @Nullable UUID lastTenantId(); @Column(name="security_version") long securityVersion();
 @Column(name="mfa_enabled") boolean mfaEnabled(); @Column(name="mfa_type") @Nullable String mfaType();
 @Column(name="last_login_at") @Nullable Instant lastLoginAt(); @Column(name="created_at") Instant createdAt(); @Column(name="updated_at") Instant updatedAt();
}
