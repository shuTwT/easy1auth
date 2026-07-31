package com.easy1auth.federation.model;
import com.easy1auth.persistence.model.BaseEntity; import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="federation_login_transaction") public interface FederationLoginTransactionEntity extends BaseEntity, BaseTenantEntity {
 @Column(name="provider_id") UUID providerId(); @Column(name="state_hash") String stateHash();
 @Column(name="nonce_hash") String nonceHash(); @Column(name="encrypted_nonce") String encryptedNonce(); @Column(name="encrypted_pkce_verifier") String encryptedPkceVerifier();
 @Nullable @Column(name="return_uri") String returnUri(); @Column(name="expires_at") Instant expiresAt(); @Nullable @Column(name="consumed_at") Instant consumedAt(); @Column(name="created_at") Instant createdAt();
}
