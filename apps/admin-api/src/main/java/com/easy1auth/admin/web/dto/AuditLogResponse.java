package com.easy1auth.admin.web.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(UUID id, UUID tenantId, UUID userId, String username, String type, String action,
                               String resource, String resourceId, String method, String ip, String userAgent,
                               Object location, String status, String errorMessage, Map<String, Object> changes,
                               Instant createdAt) {
}
