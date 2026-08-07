package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.audit.*;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {
    private final AuditService service;

    AuditController(AuditService service) {
        this.service = service;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_LIST)
    @GetMapping
    ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String username, @RequestParam(required = false) String type, @RequestParam(required = false) String action, @RequestParam(required = false) String status, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        var p = service.list(page, pageSize, new AuditService.Query(username, type, action, "failed".equals(status) ? "failure" : status, startDate, endDate));
        return ApiResponse.ok(PageData.of(p.items().stream().map(AuditController::legacy).toList(), p.page(), p.pageSize(), p.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_STATS)
    @GetMapping("/stats")
    ApiResponse<?> stats() {
        var s = service.stats();
        return ApiResponse.ok(new AuditStatsResponse(s.totalLogs(), s.successLogs(), s.failedLogs(), s.todayLogs(), 0, 0, List.of(), List.of(), List.of()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_READ)
    @GetMapping("/{id}")
    ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(legacy(service.get(id)));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_CLEANUP)
    @DeleteMapping("/cleanup")
    ApiResponse<?> cleanup(@RequestParam(defaultValue = "90") int days) {
        return ApiResponse.ok(new CleanupResponse(service.cleanup(days)), "审计日志清理完成");
    }

    private static AuditLogResponse legacy(com.easy1auth.audit.model.AuditEventEntity e) {
        return new AuditLogResponse(e.id(), e.tenantId(), e.actorId(), e.actorName(), e.eventType(), e.action(),
                e.resourceType(), e.resourceId(), e.method(), e.ipAddress() == null ? "" : e.ipAddress(),
                e.userAgent(), null, "failure".equals(e.outcome()) ? "failed" : "success", e.errorCode(),
                e.details(), e.createdAt());
    }

    public record AuditStatsResponse(long totalLogs, long successLogs, long failedLogs, long todayLogs,
                                     long weekLogs, long monthLogs, List<?> topActions, List<?> topUsers, List<?> topIps) {
    }

    public record CleanupResponse(int deletedCount) {
    }

    public record AuditLogResponse(UUID id, UUID tenantId, UUID userId, String username, String type, String action,
                                   String resource, String resourceId, String method, String ip, String userAgent,
                                   Object location, String status, String errorMessage, Map<String, Object> changes,
                                   Instant createdAt) {
    }
}
