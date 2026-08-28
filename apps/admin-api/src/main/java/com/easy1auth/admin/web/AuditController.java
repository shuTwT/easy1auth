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

/**
 * 审计日志查询接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/audit-logs}，提供审计日志的分页查询、
 * 统计汇总、单条详情与按时间清理能力。所有操作均通过
 * {@link TenantManagementPermission} 做租户级权限控制，并以 {@link ApiResponse}
 * 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {
    /** 审计日志服务 */
    private final AuditService service;

    AuditController(AuditService service) {
        this.service = service;
    }

    /** 分页查询审计日志，支持按用户名、类型、动作、状态与时间段过滤。 */
    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_LIST)
    @GetMapping
    ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String username, @RequestParam(required = false) String type, @RequestParam(required = false) String action, @RequestParam(required = false) String status, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        var p = service.list(page, pageSize, new AuditService.Query(username, type, action, "failed".equals(status) ? "failure" : status, startDate, endDate));
        return ApiResponse.ok(PageData.of(p.items().stream().map(AuditController::legacy).toList(), p.page(), p.pageSize(), p.total()));
    }

    /** 查询审计日志的整体统计汇总。 */
    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_STATS)
    @GetMapping("/stats")
    ApiResponse<?> stats() {
        var s = service.stats();
        return ApiResponse.ok(new AuditStatsResponse(s.totalLogs(), s.successLogs(), s.failedLogs(), s.todayLogs(), 0, 0, List.of(), List.of(), List.of()));
    }

    /** 查询指定审计日志的详情。 */
    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_READ)
    @GetMapping("/{id}")
    ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(legacy(service.get(id)));
    }

    /** 清理超过指定天数的历史审计日志，返回被清理的条数。 */
    @TenantManagementPermission(value = ManagementPermissionCode.AUDIT_CLEANUP)
    @DeleteMapping("/cleanup")
    ApiResponse<?> cleanup(@RequestParam(defaultValue = "90") int days) {
        return ApiResponse.ok(new CleanupResponse(service.cleanup(days)), "审计日志清理完成");
    }

    /** 把审计事件实体转换为接口层的审计日志响应（兼容旧字段命名）。 */
    private static AuditLogResponse legacy(com.easy1auth.audit.model.AuditEventEntity e) {
        return new AuditLogResponse(e.id(), e.tenantId(), e.actorId(), e.actorName(), e.eventType(), e.action(),
                e.resourceType(), e.resourceId(), e.method(), e.ipAddress() == null ? "" : e.ipAddress(),
                e.userAgent(), null, "failure".equals(e.outcome()) ? "failed" : "success", e.errorCode(),
                e.details(), e.createdAt());
    }

    /**
     * 审计统计响应。
     *
     * @param totalLogs    日志总量
     * @param successLogs  成功日志数
     * @param failedLogs   失败日志数
     * @param todayLogs    今日日志数
     * @param weekLogs     本周日志数（当前未统计，为 0）
     * @param monthLogs    本月日志数（当前未统计，为 0）
     * @param topActions   高频动作排行（当前未统计，为空）
     * @param topUsers     高频操作用户排行（当前未统计，为空）
     * @param topIps       高频来源 IP 排行（当前未统计，为空）
     */
    public record AuditStatsResponse(long totalLogs, long successLogs, long failedLogs, long todayLogs,
                                     long weekLogs, long monthLogs, List<?> topActions, List<?> topUsers, List<?> topIps) {
    }

    /**
     * 清理结果。
     *
     * @param deletedCount 被清理的日志条数
     */
    public record CleanupResponse(int deletedCount) {
    }

    /**
     * 审计日志响应（接口层视图）。
     *
     * @param id           日志 ID
     * @param tenantId     所属租户 ID
     * @param userId       操作账号 ID
     * @param username     操作账号名
     * @param type         日志类型
     * @param action       具体动作
     * @param resource     操作资源类型
     * @param resourceId   操作资源 ID
     * @param method       HTTP 方法
     * @param ip           来源 IP
     * @param userAgent    用户代理
     * @param location     位置信息（当前为空）
     * @param status       结果状态：success / failed
     * @param errorMessage 错误信息（可为 null）
     * @param changes      变更内容明细
     * @param createdAt    日志产生时间
     */
    public record AuditLogResponse(UUID id, UUID tenantId, UUID userId, String username, String type, String action,
                                   String resource, String resourceId, String method, String ip, String userAgent,
                                   Object location, String status, String errorMessage, Map<String, Object> changes,
                                   Instant createdAt) {
    }
}
