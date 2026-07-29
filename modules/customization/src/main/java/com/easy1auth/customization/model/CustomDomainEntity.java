package com.easy1auth.customization.model;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="custom_domain") public interface CustomDomainEntity {
 @Id UUID id(); @Column(name="tenant_id") UUID tenantId(); String domain(); String status(); @Column(name="verification_method") String verificationMethod();
 @Column(name="verification_token") String verificationToken(); @Nullable @Column(name="verified_at") Instant verifiedAt();
 @Column(name="created_at") Instant createdAt(); @Column(name="updated_at") Instant updatedAt();
}
