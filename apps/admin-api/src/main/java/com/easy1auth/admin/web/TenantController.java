package com.easy1auth.admin.web;

import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
import com.easy1auth.admin.security.PlatformManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import com.easy1auth.infrastructure.foundation.web.PageData;
import com.easy1auth.tenant.dto.TenantControlView;
import com.easy1auth.tenant.dto.TenantPackageView;
import com.easy1auth.tenant.dto.TenantSummary;
import com.easy1auth.tenant.service.TenantService;
import com.easy1auth.tenant.util.TenantContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * 租户管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/tenants}，提供租户列表、创建、更新、
 * 启停、管理员转移与删除等平台管理能力，也包含当前账号视角的租户查询。
 * 管理操作通过 {@link PlatformManagementPermission} 做平台级权限控制，部分
 * 面向当前账号的路由为登录后即可访问，均以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    /** 租户服务 */
    private final TenantService tenants;
    /** 平台级授权解析器 */
    private final PlatformAuthorizationResolver platformAuthorization;

    TenantController(TenantService tenants, PlatformAuthorizationResolver platformAuthorization) {
        this.tenants = tenants;
        this.platformAuthorization = platformAuthorization;
    }

    /** 查询全部租户的简略列表（平台管理视角，含系统租户）。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_LIST)
    @GetMapping("/simple-list")
    public ApiResponse<List<TenantSummary>> simpleList(Principal principal) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_LIST);
        return ApiResponse.ok(tenants.listAll());
    }

    /** 查询当前账号可访问的租户简略列表。 */
    @ManagementRouteClassification(ManagementRouteKind.DEFERRED_TODO_7)
    @GetMapping("/simple-slist/current")
    public ApiResponse<List<TenantSummary>> currentSimpleList(Principal principal) {
        return ApiResponse.ok(tenants.list(accountId(principal)));
    }

    /** 分页查询全部租户列表，支持按名称与状态过滤。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_LIST)
    @GetMapping("/list")
    public ApiResponse<PageData<TenantSummary>> list(
            Principal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_LIST);
        var rows = tenants.listAll();
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

    /** 查询可分配给普通租户的启用中套餐列表。 */
    @ManagementRouteClassification(ManagementRouteKind.DEFERRED_TODO_7)
    @GetMapping("/available-packages")
    public ApiResponse<?> availablePackages() {
        return ApiResponse.ok(tenants.listAssignablePackages().stream()
                .map(TenantPackageOption::from)
                .toList());
    }

    /** 查询全部普通租户的管理视图（用于平台侧租户管理列表）。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_LIST)
    @GetMapping("/managed")
    public ApiResponse<PageData<TenantControlView>> managed(Principal principal) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_LIST);
        var rows = tenants.listManaged();
        return ApiResponse.ok(PageData.of(rows, 1, rows.size(), rows.size()));
    }

    /** 创建普通租户并绑定套餐，当前账号自动成为租户管理员。 */
    @ManagementRouteClassification(ManagementRouteKind.DEFERRED_TODO_7)
    @PostMapping("/create")
    public ApiResponse<?> create(Principal principal, @RequestBody CreateTenant request) {
        var tenant = tenants.createOrdinary(request.name(), request.packageId(), accountId(principal));
        return ApiResponse.ok(tenant, "租户创建成功");
    }

    /** 更新指定租户的名称或绑定套餐。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_UPDATE)
    @PutMapping("/{tenantId}")
    public ApiResponse<TenantControlView> update(Principal principal, @PathVariable UUID tenantId,
                                                 @RequestBody TenantUpdateInput request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_UPDATE);
        return ApiResponse.ok(tenants.updateOrdinary(tenantId, request == null ? null : request.name(), request == null ? null : request.packageId()), "租户更新成功");
    }

    /** 更新指定租户的启停状态。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_STATUS)
    @PutMapping("/{tenantId}/status")
    public ApiResponse<TenantControlView> updateStatus(Principal principal, @PathVariable UUID tenantId,
                                                       @RequestBody TenantStatusInput request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_STATUS);
        return ApiResponse.ok(tenants.updateOrdinaryStatus(tenantId, request == null ? null : request.status()), "租户状态更新成功");
    }

    /** 转移指定租户的管理员：当前管理员降为普通成员，目标账号提升为管理员。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_OWNER_TRANSFER)
    @PutMapping("/{tenantId}/administrator")
    public ApiResponse<TenantControlView> transferAdministrator(Principal principal, @PathVariable UUID tenantId,
                                                                @RequestBody TenantAdministratorTransferInput request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_OWNER_TRANSFER);
        return ApiResponse.ok(tenants.transferAdministrator(tenantId, request == null ? null : request.administratorAccountId()), "租户管理员转移成功");
    }

    /** 删除指定普通租户（软删除）。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_DELETE)
    @DeleteMapping("/{tenantId}")
    public ApiResponse<Void> delete(Principal principal, @PathVariable UUID tenantId) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.TENANT_DELETE);
        tenants.deleteOrdinary(tenantId);
        return ApiResponse.ok(null, "租户已删除");
    }

    /** 查询当前账号在请求上下文租户中的租户摘要。 */
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

    /**
     * 租户创建请求。
     *
     * @param name                 租户名称
     * @param packageId            绑定的套餐 ID
     * @param administratorAccountId 管理员账号 ID（预留，当前以登录账号为准）
     */
    public record CreateTenant(String name, long packageId, UUID administratorAccountId) {
    }

    /**
     * 可分配套餐选项。
     *
     * @param id      套餐 ID
     * @param name    套餐名称
     * @param maxUsers 允许的最大用户数
     * @param maxApps  允许的最大应用数
     */
    public record TenantPackageOption(long id, String name, int maxUsers, int maxApps) {
        static TenantPackageOption from(TenantPackageView item) {
            return new TenantPackageOption(item.id(), item.name(), item.maxUsers(), item.maxApps());
        }
    }

    /**
     * 租户更新请求。
     *
     * @param name      新名称（可为 null，表示不修改）
     * @param packageId 新套餐 ID（可为 null，表示不修改）
     */
    public record TenantUpdateInput(String name, Long packageId) {
    }

    /**
     * 租户状态更新请求。
     *
     * @param status 目标状态：active / suspended
     */
    public record TenantStatusInput(String status) {
    }

    /**
     * 租户管理员转移请求。
     *
     * @param administratorAccountId 新管理员账号 ID
     */
    public record TenantAdministratorTransferInput(UUID administratorAccountId) {
    }
}
