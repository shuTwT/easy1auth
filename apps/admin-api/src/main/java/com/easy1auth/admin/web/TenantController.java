package com.easy1auth.admin.web;

import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
import com.easy1auth.admin.security.PlatformManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.tenant.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
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
    public ApiResponse<PageData<TenantSummary>> list(
            Principal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        var rows = tenants.list(accountId(principal));
        var filtered = rows.stream()
                .filter(tenant -> name == null || name.isBlank() || tenant.name().toLowerCase().contains(name.strip().toLowerCase()))
                .filter(tenant -> status == null || status.isBlank() || tenant.status().equals(status.strip()))
                .toList();
        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        int fromIndex = (int) Math.min((long) (normalizedPage - 1) * normalizedPageSize, filtered.size());
        int toIndex = Math.min(fromIndex + normalizedPageSize, filtered.size());
        return ApiResponse.ok(PageData.of(
                List.copyOf(filtered.subList(fromIndex, toIndex)), normalizedPage, normalizedPageSize, filtered.size()));
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_LIST)
    @GetMapping("/managed")
    public ApiResponse<PageData<TenantControlView>> managed(Principal principal) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_LIST);
        var rows = tenants.listManaged();
        return ApiResponse.ok(PageData.of(rows, 1, rows.size(), rows.size()));
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_CREATE)
    @PostMapping("/create")
    public ApiResponse<?> create(Principal principal, @RequestBody CreateTenant request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_CREATE);
        var tenant = tenants.createOrdinary(request.name(), request.packageId(), request.administratorAccountId());
        return ApiResponse.ok(tenant, "租户创建成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_UPDATE)
    @PutMapping("/{tenantId}")
    public ApiResponse<TenantControlView> update(Principal principal, @PathVariable UUID tenantId,
                                                 @RequestBody TenantUpdateInput request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_UPDATE);
        return ApiResponse.ok(tenants.updateOrdinary(tenantId, request == null ? null : request.name(), request == null ? null : request.packageId()), "租户更新成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_STATUS)
    @PutMapping("/{tenantId}/status")
    public ApiResponse<TenantControlView> updateStatus(Principal principal, @PathVariable UUID tenantId,
                                                       @RequestBody TenantStatusInput request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_STATUS);
        return ApiResponse.ok(tenants.updateOrdinaryStatus(tenantId, request == null ? null : request.status()), "租户状态更新成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_OWNER_TRANSFER)
    @PutMapping("/{tenantId}/administrator")
    public ApiResponse<TenantControlView> transferAdministrator(Principal principal, @PathVariable UUID tenantId,
                                                                @RequestBody TenantAdministratorTransferInput request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_OWNER_TRANSFER);
        return ApiResponse.ok(tenants.transferAdministrator(tenantId, request == null ? null : request.administratorAccountId()), "租户管理员转移成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_DELETE)
    @DeleteMapping("/{tenantId}")
    public ApiResponse<Void> delete(Principal principal, @PathVariable UUID tenantId) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_DELETE);
        tenants.deleteOrdinary(tenantId);
        return ApiResponse.ok(null, "租户已删除");
    }

    @ManagementRouteClassification(ManagementRouteKind.DEFERRED_TODO_7)
    @GetMapping("/current")
    public ApiResponse<?> current(Principal principal) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        var tenant = tenants.list(accountId(principal)).stream().filter(it -> it.id().equals(tenantId)).findFirst().orElseThrow();
        return ApiResponse.ok(tenant);
    }

    private static UUID accountId(Principal principal) {
        return UUID.fromString(principal.getName());
    }

    public record CreateTenant(String name, long packageId, UUID administratorAccountId) {
    }

    public record TenantUpdateInput(String name, Long packageId) {
    }

    public record TenantStatusInput(String status) {
    }

    public record TenantAdministratorTransferInput(UUID administratorAccountId) {
    }
}
