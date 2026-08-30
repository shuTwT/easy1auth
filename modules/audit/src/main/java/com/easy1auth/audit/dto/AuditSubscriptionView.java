package com.easy1auth.audit.dto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
/** Webhook 订阅视图。 */
public record AuditSubscriptionView(UUID id, UUID tenantId, String name, String url, List<String> events, String secret,
                                    String status, int maxRetries, Instant createdAt, Instant updatedAt) { }
