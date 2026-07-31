package com.easy1auth.audit.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;

import org.babyfish.jimmer.sql.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "webhook_subscription")
public interface WebhookSubscriptionEntity extends BaseEntity, BaseTenantEntity {

    String name();

    String url();

    @Serialized
    List<String> events();

    @Column(name = "secret_hash")
    String secretHash();

    @Column(name = "encrypted_secret")
    String encryptedSecret();

    String status();

    @Column(name = "max_retries")
    int maxRetries();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
