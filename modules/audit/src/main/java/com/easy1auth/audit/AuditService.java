package com.easy1auth.audit;

import com.easy1auth.audit.model.*;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.tenant.TenantContextHolder;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class AuditService {
    private static final AuditEventEntityTable EVENT = AuditEventEntityTable.$;
    private static final Set<String> SECRET_KEYS = Set.of("password", "token", "secret", "code", "authorization", "cookie", "privateKey", "refreshToken");
    private final JSqlClient sql;

    public AuditService(JSqlClient sql) {
        this.sql = sql;
    }

    @Transactional
    public UUID record(Event input) {
        UUID id = UuidV7.randomUuid();
        var e = AuditEventEntityDraft.$.produce(d -> d.setId(id).setTenantId(input.tenantId()).setActorType(clean(input.actorType(), 24, "system")).setActorId(input.actorId()).setActorName(trim(input.actorName(), 320)).setEventType(clean(input.eventType(), 100, "security")).setAction(clean(input.action(), 100, "unknown")).setResourceType(clean(input.resourceType(), 100, "unknown")).setResourceId(trim(input.resourceId(), 200)).setTraceId(trim(input.traceId(), 100)).setMethod(trim(input.method(), 16)).setIpAddress(trim(input.ipAddress(), 64)).setUserAgent(trim(input.userAgent(), 512)).setOutcome("failure".equals(input.outcome()) ? "failure" : "success").setErrorCode(trim(input.errorCode(), 100)).setDetails(redact(input.details())).setCreatedAt(Instant.now()));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return id;
    }

    @Transactional(readOnly = true)
    public Page list(int page, int size, Query q) {
        UUID tenant = TenantContextHolder.requireTenantId();
        int p = Math.max(1, page), s = Math.min(200, Math.max(1, size));
        var query = sql.createQuery(EVENT).where(EVENT.tenantId().eq(tenant)).whereIf(q != null && q.actorName() != null && !q.actorName().isBlank(), () -> EVENT.actorName().ilike(q.actorName(), LikeMode.ANYWHERE)).whereIf(q != null && q.eventType() != null && !q.eventType().isBlank(), () -> EVENT.eventType().eq(q.eventType())).whereIf(q != null && q.action() != null && !q.action().isBlank(), () -> EVENT.action().eq(q.action())).whereIf(q != null && q.outcome() != null && !q.outcome().isBlank(), () -> EVENT.outcome().eq(q.outcome())).whereIf(q != null && q.start() != null, () -> EVENT.createdAt().ge(q.start())).whereIf(q != null && q.end() != null, () -> EVENT.createdAt().le(q.end())).orderBy(EVENT.createdAt().desc()).select(EVENT);
        long total = query.fetchUnlimitedCount();
        return new Page(query.limit(s, (long) (p - 1) * s).execute(), total, p, s);
    }

    @Transactional(readOnly = true)
    public AuditEventEntity get(UUID id) {
        UUID tenant = TenantContextHolder.requireTenantId();
        return sql.createQuery(EVENT).where(EVENT.tenantId().eq(tenant), EVENT.id().eq(id)).select(EVENT).fetchOptional().orElseThrow(() -> new DomainException("AUDIT_EVENT_NOT_FOUND", "审计事件不存在", 404));
    }

    @Transactional
    public int cleanup(int days) {
        UUID tenant = TenantContextHolder.requireTenantId();
        if (days < 30) throw new DomainException("AUDIT_RETENTION_INVALID", "审计保留期不能少于30天", 400);
        return sql.createDelete(EVENT).where(EVENT.tenantId().eq(tenant), EVENT.createdAt().lt(Instant.now().minus(Duration.ofDays(days)))).execute();
    }

    @Transactional(readOnly = true)
    public Stats stats() {
        UUID tenant = TenantContextHolder.requireTenantId();
        var rows = sql.createQuery(EVENT).where(EVENT.tenantId().eq(tenant)).select(EVENT.outcome(), EVENT.createdAt()).execute();
        Instant day = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new Stats(rows.size(), rows.stream().filter(r -> "success".equals(r.get_1())).count(), rows.stream().filter(r -> "failure".equals(r.get_1())).count(), rows.stream().filter(r -> r.get_2().isAfter(day)).count());
    }

    private static Map<String, Object> redact(Map<String, Object> in) {
        if (in == null) return Map.of();
        Map<String, Object> out = new LinkedHashMap<>();
        in.forEach((k, v) -> out.put(k, SECRET_KEYS.stream().anyMatch(x -> x.equalsIgnoreCase(k)) ? "[REDACTED]" : v));
        return out;
    }

    private static String clean(String v, int max, String fallback) {
        String value = v == null || v.isBlank() ? fallback : v;
        return trim(value, max);
    }

    private static String trim(String v, int max) {
        return v == null ? null : v.substring(0, Math.min(max, v.length()));
    }

    public record Event(UUID tenantId, String actorType, UUID actorId, String actorName, String eventType,
                        String action, String resourceType, String resourceId, String traceId, String method,
                        String ipAddress, String userAgent, String outcome, String errorCode,
                        Map<String, Object> details) {
    }

    public record Query(String actorName, String eventType, String action, String outcome, Instant start, Instant end) {
    }

    public record Page(List<AuditEventEntity> logs, long total, int page, int pageSize) {
    }

    public record Stats(long totalLogs, long successLogs, long failedLogs, long todayLogs) {
    }
}
