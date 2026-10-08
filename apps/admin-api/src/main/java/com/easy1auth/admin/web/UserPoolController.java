package com.easy1auth.admin.web;

import com.easy1auth.admin.web.dto.*;
import com.easy1auth.admin.annotation.ManagementRouteClassification;
import com.easy1auth.admin.constant.ManagementRouteKind;
import com.easy1auth.admin.annotation.PlatformManagementPermission;
import com.easy1auth.system.constant.ManagementPermissionCode;
import com.easy1auth.system.PlatformAuthorizationResolver;
import com.easy1auth.framework.web.response.ApiResponse;
import com.easy1auth.framework.common.pagination.PageData;
import com.easy1auth.tenant.dto.UserPoolControlView;
import com.easy1auth.tenant.dto.UserPoolSummary;
import com.easy1auth.tenant.service.UserPoolService;
import com.easy1auth.framework.tenant.context.UserPoolContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * 用户池管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/user-pools}，提供用户池列表、创建、更新、
 * 启停、管理员转移与删除等平台管理能力，也包含当前账号视角的用户池查询。
 * 管理操作通过 {@link PlatformManagementPermission} 做平台级权限控制，部分
 * 面向当前账号的路由为登录后即可访问，均以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping({"/api/user-pools", "/api/tenants"})
public class UserPoolController {
    /** 用户池服务 */
    private final UserPoolService userPools;
    /** 平台级授权解析器 */
    private final PlatformAuthorizationResolver platformAuthorization;

    UserPoolController(UserPoolService userPools, PlatformAuthorizationResolver platformAuthorization) {
        this.userPools = userPools;
        this.platformAuthorization = platformAuthorization;
    }

    /** 查询全部用户池的简略列表（平台管理视角，含系统用户池）。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.USER_POOL_LIST)
    @GetMapping("/simple-list")
    public ApiResponse<List<UserPoolSummary>> simpleList(Principal principal) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.USER_POOL_LIST);
        return ApiResponse.ok(userPools.listAll());
    }

    /** 查询当前账号可访问的用户池简略列表。 */
    @ManagementRouteClassification(ManagementRouteKind.DEFERRED_TODO_7)
    @GetMapping("/simple-slist/current")
    public ApiResponse<List<UserPoolSummary>> currentSimpleList(Principal principal) {
        return ApiResponse.ok(userPools.list(accountId(principal)));
    }

    /** 分页查询全部用户池列表，支持按名称与状态过滤。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.USER_POOL_LIST)
    @GetMapping("/list")
    public ApiResponse<PageData<UserPoolSummary>> list(
            Principal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.USER_POOL_LIST);
        var rows = userPools.listAll();
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

    /** 查询可分配给普通用户池的启用中套餐列表。 */
    @ManagementRouteClassification(ManagementRouteKind.DEFERRED_TODO_7)
    @GetMapping("/available-packages")
    public ApiResponse<?> availablePackages() {
        return ApiResponse.ok(userPools.listAssignablePackages().stream()
                .map(UserPoolPackageOption::from)
                .toList());
    }

    /** 查询全部普通用户池的管理视图（用于平台侧用户池管理列表）。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.USER_POOL_LIST)
    @GetMapping("/managed")
    public ApiResponse<PageData<UserPoolControlView>> managed(Principal principal) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.USER_POOL_LIST);
        var rows = userPools.listManaged();
        return ApiResponse.ok(PageData.of(rows, 1, rows.size(), rows.size()));
    }

    /** 创建普通用户池并绑定套餐，当前账号自动成为用户池管理员。 */
    @ManagementRouteClassification(ManagementRouteKind.DEFERRED_TODO_7)
    @PostMapping("/create")
    public ApiResponse<?> create(Principal principal, @RequestBody CreateUserPool request) {
        var tenant = userPools.createOrdinary(request.name(), request.packageId(), accountId(principal));
        return ApiResponse.ok(tenant, "用户池创建成功");
    }

    /** 更新指定用户池的名称或绑定套餐。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.USER_POOL_UPDATE)
    @PutMapping("/{userPoolId}")
    public ApiResponse<UserPoolControlView> update(Principal principal, @PathVariable UUID userPoolId,
                                                 @RequestBody UserPoolUpdateInput request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.USER_POOL_UPDATE);
        return ApiResponse.ok(userPools.updateOrdinary(userPoolId, request == null ? null : request.name(), request == null ? null : request.packageId()), "用户池更新成功");
    }

    /** 更新指定用户池的启停状态。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.USER_POOL_STATUS)
    @PutMapping("/{userPoolId}/status")
    public ApiResponse<UserPoolControlView> updateStatus(Principal principal, @PathVariable UUID userPoolId,
                                                       @RequestBody UserPoolStatusInput request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.USER_POOL_STATUS);
        return ApiResponse.ok(userPools.updateOrdinaryStatus(userPoolId, request == null ? null : request.status()), "用户池状态更新成功");
    }

    /** 转移指定用户池的管理员：当前管理员降为普通成员，目标账号提升为管理员。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.USER_POOL_OWNER_TRANSFER)
    @PutMapping("/{userPoolId}/administrator")
    public ApiResponse<UserPoolControlView> transferAdministrator(Principal principal, @PathVariable UUID userPoolId,
                                                                @RequestBody UserPoolAdministratorTransferInput request) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.USER_POOL_OWNER_TRANSFER);
        return ApiResponse.ok(userPools.transferAdministrator(userPoolId, request == null ? null : request.administratorAccountId()), "用户池管理员转移成功");
    }

    /** 删除指定普通用户池（软删除）。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.USER_POOL_DELETE)
    @DeleteMapping("/{userPoolId}")
    public ApiResponse<Void> delete(Principal principal, @PathVariable UUID userPoolId) {
        platformAuthorization.require(accountId(principal), ManagementPermissionCode.USER_POOL_DELETE);
        userPools.deleteOrdinary(userPoolId);
        return ApiResponse.ok(null, "用户池已删除");
    }

    /** 查询当前账号在请求上下文用户池中的用户池摘要。 */
    @ManagementRouteClassification(ManagementRouteKind.DEFERRED_TODO_7)
    @GetMapping("/current")
    public ApiResponse<?> current(Principal principal) {
        UUID userPoolId = UserPoolContextHolder.requireUserPoolId();
        var tenant = userPools.list(accountId(principal)).stream().filter(it -> it.id().equals(userPoolId)).findFirst().orElseThrow();
        return ApiResponse.ok(tenant);
    }

    private static UUID accountId(Principal principal) {
        return UUID.fromString(principal.getName());
    }

    /**
     * 用户池创建请求。
     *
     * @param name                 用户池名称
     * @param packageId            绑定的套餐 ID
     * @param administratorAccountId 管理员账号 ID（预留，当前以登录账号为准）
     */

    /**
     * 可分配套餐选项。
     *
     * @param id      套餐 ID
     * @param name    套餐名称
     * @param maxUsers 允许的最大用户数
     * @param maxApps  允许的最大应用数
     */

    /**
     * 用户池更新请求。
     *
     * @param name      新名称（可为 null，表示不修改）
     * @param packageId 新套餐 ID（可为 null，表示不修改）
     */

    /**
     * 用户池状态更新请求。
     *
     * @param status 目标状态：active / suspended
     */

    /**
     * 用户池管理员转移请求。
     *
     * @param administratorAccountId 新管理员账号 ID
     */
}
