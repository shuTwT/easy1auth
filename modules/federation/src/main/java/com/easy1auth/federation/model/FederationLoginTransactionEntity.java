package com.easy1auth.federation.model;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="federation_login_transaction") public interface FederationLoginTransactionEntity {
 @Id UUID id(); @Column(name="tenant_id") UUID tenantId(); @Column(name="provider_id") UUID providerId(); @Column(name="state_hash") String stateHash();
 @Column(name="nonce_hash") String nonceHash(); @Column(name="encrypted_nonce") String encryptedNonce(); @Column(name="encrypted_pkce_verifier") String encryptedPkceVerifier();
 @Nullable @Column(name="return_uri") String returnUri(); @Column(name="expires_at") Instant expiresAt(); @Nullable @Column(name="consumed_at") Instant consumedAt(); @Column(name="created_at") Instant createdAt();
}
