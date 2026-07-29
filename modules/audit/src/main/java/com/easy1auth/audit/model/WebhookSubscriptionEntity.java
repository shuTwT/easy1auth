package com.easy1auth.audit.model;
import org.babyfish.jimmer.sql.*; import java.time.Instant; import java.util.*;
@Entity @Table(name="webhook_subscription") public interface WebhookSubscriptionEntity {
 @Id UUID id(); @Column(name="tenant_id") UUID tenantId(); String name(); String url(); @Serialized List<String> events();
 @Column(name="secret_hash") String secretHash(); @Column(name="encrypted_secret") String encryptedSecret(); String status();
 @Column(name="max_retries") int maxRetries(); @Column(name="created_at") Instant createdAt(); @Column(name="updated_at") Instant updatedAt();
}
