package com.easy1auth.admin.web;

import com.easy1auth.admin.web.dto.*;
import com.easy1auth.admin.annotation.TenantManagementPermission;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.application.service.ApplicationService;
import com.easy1auth.poolidentity.service.PoolUserService;
import com.easy1auth.common.foundation.web.ApiResponse;
import com.easy1auth.tenant.service.TenantService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * 管理端首页看板接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/dashboard}，汇总当前账号的租户数、
 * 用户数、应用数与今日登录数等统计信息，并返回最近登录动态。通过
 * {@link TenantManagementPermission} 做租户级权限控制，以 {@link ApiResponse}
 * 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    /** 租户服务 */
    private final TenantService tenants;
    /** 目录用户（pool_user）服务 */
    private final PoolUserService users;
    /** 应用服务 */
    private final ApplicationService applications;

    DashboardController(TenantService tenants, PoolUserService users, ApplicationService applications) {
        this.tenants = tenants;
        this.users = users;
        this.applications = applications;
    }

    /** 查询首页看板统计信息与最近登录动态。 */
    @TenantManagementPermission(ManagementPermissionCode.MENU_DASHBOARD)
    @GetMapping("/stats")
    ApiResponse<DashboardData> stats(@AuthenticationPrincipal Jwt actor) {
        var userStats = users.stats();
        var applicationStats = applications.stats();
        Instant today = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<com.easy1auth.poolidentity.dto.RecentLogin> loginRows = users.recentLogins(20);
        List<RecentLogin> recentLogins = loginRows.stream()
                .limit(5)
                .map(login -> new RecentLogin(login.username(), login.email(), "", login.time(), "success"))
                .toList();
        return ApiResponse.ok(new DashboardData(
                new DashboardStats(
                        tenants.list(UUID.fromString(actor.getSubject())).size(),
                        userStats.totalUsers(),
                        applicationStats.totalApplications(),
                        users.successfulLoginCountSince(today)),
                recentLogins));
    }

    /**
     * 看板数据。
     *
     * @param stats        统计信息
     * @param recentLogins 最近登录动态列表
     */

    /**
     * 看板统计信息。
     *
     * @param tenantCount      当前账号可访问的租户数量
     * @param userCount        目录用户总数
     * @param applicationCount 应用总数
     * @param todayLoginCount  今日成功登录次数
     */

    /**
     * 最近一次登录动态。
     *
     * @param username 登录用户名
     * @param email    登录邮箱（可为 null）
     * @param ip       登录来源 IP
     * @param time     登录时间
     * @param status   登录结果状态：success / failed
     */
}
