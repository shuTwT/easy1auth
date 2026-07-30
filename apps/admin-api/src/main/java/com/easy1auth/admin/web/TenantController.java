package com.easy1auth.admin.web;

import com.easy1auth.foundation.trace.TraceIdFilter;
import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
import com.easy1auth.admin.security.PlatformManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.tenant.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantService tenants;
    private final PlatformAuthorizationResolver platformAuthorization;
    TenantController(TenantService tenants, PlatformAuthorizationResolver platformAuthorization) {
        this.tenants = tenants;
        this.platformAuthorization = platformAuthorization;
    }

    @ManagementRouteClassification(ManagementRouteKind.DEFERRED_TODO_7)
    @GetMapping("/list")
    public ApiResponse<PageData<TenantSummary>> list(Principal principal) {
        var rows = tenants.list(accountId(principal));
        return ApiResponse.ok(PageData.of(rows, 1, rows.size(), rows.size()));
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_CREATE, boundary = TenantDataBoundary.PLATFORM_ALL)
    @PostMapping("/create")
    public ApiResponse<?> create(Principal principal, @RequestBody CreateTenant request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_CREATE);
        var tenant = tenants.createOrdinary(request.name(), request.packageId(), request.administratorAccountId());
        return ApiResponse.ok(tenant, "租户创建成功");
    }

    @ManagementRouteClassification(ManagementRouteKind.DEFERRED_TODO_7)
    @GetMapping("/current")
    public ApiResponse<?> current(Principal principal, @RequestHeader("tenant-id") UUID tenantId,
                                       HttpServletRequest request) {
        var context = tenants.resolve(accountId(principal), tenantId, traceId(request));
        var tenant = tenants.list(context.accountId()).stream().filter(it -> it.id().equals(tenantId)).findFirst().orElseThrow();
        return ApiResponse.ok(tenant);
    }

    private static UUID accountId(Principal principal) { return UUID.fromString(principal.getName()); }
    private static String traceId(HttpServletRequest request) { return request.getHeader(TraceIdFilter.HEADER); }
    public record CreateTenant(String name, long packageId, UUID administratorAccountId) {}
}
