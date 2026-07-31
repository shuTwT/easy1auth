package com.easy1auth.enterpriseidentity.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "enterprise_identity_source")
public interface EnterpriseIdentitySourceEntity extends BaseEntity, BaseTenantEntity {
    String name();
    String provider();
    @Column(name = "app_id") String appId();
    @Column(name = "encrypted_app_secret") String encryptedAppSecret();
    @Column(name = "encrypted_verification_token") String encryptedVerificationToken();
    @Column(name = "encrypted_encrypt_key") String encryptedEncryptKey();
    String status();
    @Nullable @Column(name = "last_sync_at") Instant lastSyncAt();
    @Nullable @Column(name = "last_sync_status") String lastSyncStatus();
    @Nullable @Column(name = "last_error") String lastError();
    @Column(name = "created_at") Instant createdAt();
    @Column(name = "updated_at") Instant updatedAt();
}
