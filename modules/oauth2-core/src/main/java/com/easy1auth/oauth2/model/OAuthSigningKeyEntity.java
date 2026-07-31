package com.easy1auth.oauth2.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name="oauth2_signing_key")
public interface OAuthSigningKeyEntity extends BaseEntity, BaseTenantEntity {
    @Column(name="key_id") String keyId();
    String algorithm();
    @Serialized @Column(name="public_jwk") Map<String,Object> publicJwk();
    @Column(name="encrypted_private_jwk") String encryptedPrivateJwk();
    String status();
    @Column(name="created_at") Instant createdAt();
    @Column(name="expires_at") @Nullable Instant expiresAt();
}
