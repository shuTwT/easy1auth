package com.easy1auth.federation.model;
import org.babyfish.jimmer.sql.*; import java.time.Instant; import java.util.*;
@Entity @Table(name="federation_provider") public interface FederationProviderEntity {
 @Id UUID id(); @Column(name="tenant_id") UUID tenantId(); String name(); String issuer(); @Column(name="client_id") String clientId();
 @Column(name="encrypted_client_secret") String encryptedClientSecret(); @Serialized List<String> scopes(); @Serialized @Column(name="claim_mapping") Map<String,String> claimMapping();
 @Column(name="jit_provisioning") boolean jitProvisioning(); String status(); @Column(name="created_at") Instant createdAt(); @Column(name="updated_at") Instant updatedAt();
}
