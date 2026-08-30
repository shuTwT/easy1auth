package com.easy1auth.social.dto;
import java.time.Instant;
import java.util.UUID;
/** 社交身份源视图。 */
public record SourceView(UUID id, UUID tenantId, String name, String type, String mode, String clientId,
                         String clientSecret, boolean jitProvisioning, String status, Instant createdAt, Instant updatedAt) { }
