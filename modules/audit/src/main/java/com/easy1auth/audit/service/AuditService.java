package com.easy1auth.audit.service;

import com.easy1auth.audit.ErrorCodeConstants;
import com.easy1auth.audit.Event;
import com.easy1auth.audit.Query;
import com.easy1auth.audit.Stats;
import com.easy1auth.audit.model.*;
import com.easy1auth.audit.repository.AuditEventRepository;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.id.UuidV7;
import com.easy1auth.infrastructure.foundation.web.PageData;
import com.easy1auth.tenant.util.TenantContextHolder;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * 审计服务：审计事件的记录、查询与清理。
 *
 * <p>负责写入审计事件（自动脱敏敏感字段、规范化枚举取值），并支持按条件分页
 * 查询、详情查看、超期清理与基础统计。查询与清理均以当前租户隔离，保证租户
 * 只能访问自己的审计数据。</p>
 */
@Service
public class AuditService {
    /** audit_event 表静态描述符 */
    private static final AuditEventEntityTable EVENT = AuditEventEntityTable.$;
    /** 需在审计详情中脱敏的敏感字段名（大小写不敏感） */
    private static final Set<String> SECRET_KEYS = Set.of("password", "token", "secret", "code", "authorization", "cookie", "privateKey", "refreshToken");
    /** jimmer SQL 客户端 */
    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /** 记录一条审计事件并返回事件 ID（敏感字段自动脱敏）。 */
    @Transactional
    public UUID record(Event input) {
        UUID id = UuidV7.randomUuid();
        var e = AuditEventEntityDraft.$.produce(d -> d.setId(id).setTenantId(input.tenantId()).setActorType(clean(input.actorType(), 24, "system")).setActorId(input.actorId()).setActorName(trim(input.actorName(), 320)).setEventType(clean(input.eventType(), 100, "security")).setAction(clean(input.action(), 100, "unknown")).setResourceType(clean(input.resourceType(), 100, "unknown")).setResourceId(trim(input.resourceId(), 200)).setTraceId(trim(input.traceId(), 100)).setMethod(trim(input.method(), 16)).setIpAddress(trim(input.ipAddress(), 64)).setUserAgent(trim(input.userAgent(), 512)).setOutcome("failure".equals(input.outcome()) ? "failure" : "success").setErrorCode(trim(input.errorCode(), 100)).setDetails(redact(input.details())).setCreatedAt(Instant.now()));
        repository.sql().saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return id;
    }

    /** 分页查询当前租户的审计事件（支持按操作者、事件类型、动作、结果与时间范围过滤）。 */
    @Transactional(readOnly = true)
    public PageData<AuditEventEntity> list(int page, int size, Query q) {
        UUID tenant = TenantContextHolder.requireTenantId();
        int p = Math.max(1, page), s = Math.min(200, Math.max(1, size));
        var query = repository.sql().createQuery(EVENT).where(EVENT.tenantId().eq(tenant)).whereIf(q != null && q.actorName() != null && !q.actorName().isBlank(), () -> EVENT.actorName().ilike(q.actorName(), LikeMode.ANYWHERE)).whereIf(q != null && q.eventType() != null && !q.eventType().isBlank(), () -> EVENT.eventType().eq(q.eventType())).whereIf(q != null && q.action() != null && !q.action().isBlank(), () -> EVENT.action().eq(q.action())).whereIf(q != null && q.outcome() != null && !q.outcome().isBlank(), () -> EVENT.outcome().eq(q.outcome())).whereIf(q != null && q.start() != null, () -> EVENT.createdAt().ge(q.start())).whereIf(q != null && q.end() != null, () -> EVENT.createdAt().le(q.end())).orderBy(EVENT.createdAt().desc()).select(EVENT);
        long total = query.fetchUnlimitedCount();
        return PageData.of(query.limit(s, (long) (p - 1) * s).execute(), p, s, total);
    }

    /** 查询单条审计事件详情（限当前租户）。 */
    @Transactional(readOnly = true)
    public AuditEventEntity get(UUID id) {
        UUID tenant = TenantContextHolder.requireTenantId();
        return repository.sql().createQuery(EVENT).where(EVENT.tenantId().eq(tenant), EVENT.id().eq(id)).select(EVENT).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.AUDIT_EVENT_NOT_FOUND));
    }

    /** 清理当前租户早于保留期（不少于 30 天）的审计事件，返回删除条数。 */
    @Transactional
    public int cleanup(int days) {
        UUID tenant = TenantContextHolder.requireTenantId();
        if (days < 30) {
            throw new DomainException(ErrorCodeConstants.AUDIT_RETENTION_INVALID);
        }
        return repository.sql().createDelete(EVENT).where(EVENT.tenantId().eq(tenant), EVENT.createdAt().lt(Instant.now().minus(Duration.ofDays(days)))).execute();
    }

    /** 统计当前租户审计事件总量、成功/失败数与今日发生数。 */
    @Transactional(readOnly = true)
    public Stats stats() {
        UUID tenant = TenantContextHolder.requireTenantId();
        var rows = repository.sql().createQuery(EVENT).where(EVENT.tenantId().eq(tenant)).select(EVENT.outcome(), EVENT.createdAt()).execute();
        Instant day = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new Stats(rows.size(), rows.stream().filter(r -> "success".equals(r.get_1())).count(), rows.stream().filter(r -> "failure".equals(r.get_1())).count(), rows.stream().filter(r -> r.get_2().isAfter(day)).count());
    }

    /** 将详情中命中敏感键名的值替换为 [REDACTED]，其余原样保留。 */
    private static Map<String, Object> redact(Map<String, Object> in) {
        if (in == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        in.forEach((k, v) -> out.put(k, SECRET_KEYS.stream().anyMatch(x -> x.equalsIgnoreCase(k)) ? "[REDACTED]" : v));
        return out;
    }

    /** 规范化字符串：空值回退为默认值，再截断到最大长度。 */
    private static String clean(String v, int max, String fallback) {
        String value = v == null || v.isBlank() ? fallback : v;
        return trim(value, max);
    }

    /** 将字符串截断到最大长度（null 原样返回）。 */
    private static String trim(String v, int max) {
        return v == null ? null : v.substring(0, Math.min(max, v.length()));
    }

}
