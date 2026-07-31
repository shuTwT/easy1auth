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
        return ApiResponse.ok(PageData.of(p.logs().stream().map(AuditController::legacy).toList(), p.page(), p.pageSize(), p.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_STATS)
    @GetMapping("/stats")
    ApiResponse<?> stats() {
        var s = service.stats();
        return ApiResponse.ok(Map.of("totalLogs", s.totalLogs(), "successLogs", s.successLogs(), "failedLogs", s.failedLogs(), "todayLogs", s.todayLogs(), "weekLogs", 0, "monthLogs", 0, "topActions", List.of(), "topUsers", List.of(), "topIps", List.of()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_READ)
    @GetMapping("/{id}")
    ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(legacy(service.get(id)));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_CLEANUP)
    @DeleteMapping("/cleanup")
    ApiResponse<?> cleanup(@RequestParam(defaultValue = "90") int days) {
        return ApiResponse.ok(Map.of("deletedCount", service.cleanup(days)), "审计日志清理完成");
    }

    private static Map<String, Object> legacy(com.easy1auth.audit.model.AuditEventEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.id());
        m.put("tenantId", e.tenantId());
        m.put("userId", e.actorId());
        m.put("username", e.actorName());
        m.put("type", e.eventType());
        m.put("action", e.action());
        m.put("resource", e.resourceType());
        m.put("resourceId", e.resourceId());
        m.put("method", e.method());
        m.put("ip", e.ipAddress() == null ? "" : e.ipAddress());
        m.put("userAgent", e.userAgent());
        m.put("location", null);
        m.put("status", "failure".equals(e.outcome()) ? "failed" : "success");
        m.put("errorMessage", e.errorCode());
        m.put("changes", e.details());
        m.put("createdAt", e.createdAt());
        return m;
    }
}
