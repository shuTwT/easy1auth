package com.easy1auth.adminidentity.model;
import org.babyfish.jimmer.sql.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="admin_credential") public interface AdminCredentialEntity{
 @Id @Column(name="account_id") UUID accountId(); @Column(name="password_hash") String passwordHash();
 @Column(name="password_changed_at") Instant passwordChangedAt(); @Column(name="created_at") Instant createdAt(); @Column(name="updated_at") Instant updatedAt();
}
