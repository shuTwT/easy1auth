package com.easy1auth.audit.model;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.*;
@Entity @Table(name="audit_event") public interface AuditEventEntity {
 @Id UUID id(); @Nullable @Column(name="tenant_id") UUID tenantId(); @Column(name="actor_type") String actorType();
 @Nullable @Column(name="actor_id") UUID actorId(); @Nullable @Column(name="actor_name") String actorName(); @Column(name="event_type") String eventType();
 String action(); @Column(name="resource_type") String resourceType(); @Nullable @Column(name="resource_id") String resourceId();
 @Nullable @Column(name="trace_id") String traceId(); @Nullable String method(); @Nullable @Column(name="ip_address") String ipAddress();
 @Nullable @Column(name="user_agent") String userAgent(); String outcome(); @Nullable @Column(name="error_code") String errorCode();
 @Serialized Map<String,Object> details(); @Column(name="created_at") Instant createdAt();
}
