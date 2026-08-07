package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.application.ApplicationService;
import com.easy1auth.directory.PoolUserService;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.TenantService;
import org.jspecify.annotations.Nullable;
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

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final TenantService tenants;
    private final PoolUserService users;
    private final ApplicationService applications;

    DashboardController(TenantService tenants, PoolUserService users, ApplicationService applications) {
        this.tenants = tenants;
        this.users = users;
        this.applications = applications;
    }

    @TenantManagementPermission(ManagementPermissionCode.MENU_DASHBOARD)
    @GetMapping("/stats")
    ApiResponse<DashboardData> stats(@AuthenticationPrincipal Jwt actor) {
        var userStats = users.stats();
        var applicationStats = applications.stats();
        Instant today = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<PoolUserService.RecentLogin> loginRows = users.recentLogins(20);
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

    public record DashboardData(DashboardStats stats, List<RecentLogin> recentLogins) {
    }

    public record DashboardStats(long tenantCount, long userCount, long applicationCount, long todayLoginCount) {
    }

    public record RecentLogin(String username, @Nullable String email, String ip, Instant time, String status) {
    }
}
