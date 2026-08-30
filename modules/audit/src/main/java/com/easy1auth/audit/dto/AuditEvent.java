package com.easy1auth.audit.dto;
import java.util.Map;
import java.util.UUID;
/** 审计事件。 */
public record AuditEvent(UUID tenantId, String actorType, UUID actorId, String actorName, String eventType,
                         String action, String resourceType, String resourceId, String traceId, String method,
                         String ipAddress, String userAgent, String outcome, String errorCode,
                         Map<String, Object> details) { }
