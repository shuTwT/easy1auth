package com.easy1auth.audit.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "delivery_outbox")
public interface DeliveryOutboxEntity {
    @Id
    UUID id();

    @Nullable
    @Column(name = "tenant_id")
    UUID tenantId();

    String channel();

    String destination();

    @Column(name = "event_type")
    String eventType();

    @Serialized
    Map<String, Object> payload();

    @Nullable
    @Column(name = "subscription_id")
    UUID subscriptionId();

    @Column(name = "idempotency_key")
    String idempotencyKey();

    String status();

    int attempts();

    @Column(name = "max_attempts")
    int maxAttempts();

    @Column(name = "available_at")
    Instant availableAt();

    @Nullable
    @Column(name = "lease_until")
    Instant leaseUntil();

    @Nullable
    @Column(name = "last_error")
    String lastError();

    @Column(name = "created_at")
    Instant createdAt();

    @Nullable
    @Column(name = "sent_at")
    Instant sentAt();
}
