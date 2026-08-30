package com.easy1auth.admin.web;

import com.easy1auth.admin.constant.ErrorCodeConstants;
import com.easy1auth.admin.web.dto.*;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.admin.security.PlatformManagementPermission;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import com.easy1auth.tenant.dto.TenantPackageMutation;
import com.easy1auth.tenant.service.TenantPackageService;
import com.easy1auth.tenant.dto.TenantPackageView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 平台租户套餐管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/platform/tenant-packages}，提供租户套餐
 * 的分页查询、详情、创建、更新、启停、权限替换与删除等平台管理能力。所有操作均
 * 通过 {@link PlatformManagementPermission} 做平台级权限控制，并以 {@link ApiResponse}
 * 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/platform/tenant-packages")
public class TenantPackageController {
    /** 平台级授权解析器 */
    private final PlatformAuthorizationResolver platformAuthorization;
    /** 租户套餐服务 */
    private final TenantPackageService packages;

    TenantPackageController(PlatformAuthorizationResolver platformAuthorization, TenantPackageService packages) {
        this.platformAuthorization = platformAuthorization;
        this.packages = packages;
    }

    /** 查询全部租户套餐列表。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_LIST)
    @GetMapping
    public ApiResponse<List<TenantPackageView>> list(@AuthenticationPrincipal Jwt actor) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_LIST);
        return ApiResponse.ok(packages.list());
    }

    /** 查询指定租户套餐的详情。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_READ)
    @GetMapping("/{packageId}")
    public ApiResponse<TenantPackageView> get(@AuthenticationPrincipal Jwt actor, @PathVariable long packageId) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_READ);
        return ApiResponse.ok(packages.get(packageId));
    }

    /** 创建租户套餐。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_CREATE)
    @PostMapping
    public ApiResponse<TenantPackageView> create(@AuthenticationPrincipal Jwt actor, @RequestBody TenantPackageInput input) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_CREATE);
        return ApiResponse.ok(packages.create(mutation(input)), "租户套餐创建成功");
    }

    /** 更新指定租户套餐的配置。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_UPDATE)
    @PutMapping("/{packageId}")
    public ApiResponse<TenantPackageView> update(@AuthenticationPrincipal Jwt actor, @PathVariable long packageId,
                                                 @RequestBody TenantPackageInput input) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_UPDATE);
        return ApiResponse.ok(packages.update(packageId, mutation(input)), "租户套餐更新成功");
    }

    /** 更新指定租户套餐的启停状态。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_STATUS)
    @PutMapping("/{packageId}/status")
    public ApiResponse<TenantPackageView> updateStatus(@AuthenticationPrincipal Jwt actor, @PathVariable long packageId,
                                                       @RequestBody TenantPackageStatusInput input) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_STATUS);
        return ApiResponse.ok(packages.updateStatus(packageId, input == null ? null : input.status()), "租户套餐状态更新成功");
    }

    /** 全量替换指定租户套餐绑定的权限集合。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_PERMISSION_REPLACE)
    @PutMapping("/{packageId}/permissions")
    public ApiResponse<TenantPackageView> replacePermissions(@AuthenticationPrincipal Jwt actor, @PathVariable long packageId,
                                                             @RequestBody TenantPackagePermissionsInput input) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_PERMISSION_REPLACE);
        return ApiResponse.ok(packages.replacePermissions(packageId, input == null ? null : input.permissionCodes()), "租户套餐权限更新成功");
    }

    /** 删除指定租户套餐。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_DELETE)
    @DeleteMapping("/{packageId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt actor, @PathVariable long packageId) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_DELETE);
        packages.delete(packageId);
        return ApiResponse.ok(null, "租户套餐删除成功");
    }

    /** 校验当前账号是否具备指定平台权限，不具备时抛出授权异常。 */
    private void require(Jwt actor, ManagementPermissionCode permission) {
        platformAuthorization.require(accountId(actor), permission);
    }

    /** 从 JWT 主体解析当前账号 ID，缺失或非法时抛出认证异常。 */
    private static UUID accountId(Jwt actor) {
        if (actor == null || actor.getSubject() == null) {
            throw new DomainException(ErrorCodeConstants.AUTHENTICATION_SUBJECT_INVALID);
        }
        try {
            return UUID.fromString(actor.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new DomainException(ErrorCodeConstants.AUTHENTICATION_SUBJECT_INVALID);
        }
    }

    /** 将套餐输入转换为服务层变更对象，输入为空时抛出套餐缺失异常。 */
    private static TenantPackageMutation mutation(TenantPackageInput input) {
        if (input == null) {
            throw new DomainException(com.easy1auth.tenant.constant.ErrorCodeConstants.TENANT_PACKAGE_REQUIRED);
        }
        return input.toMutation();
    }

    /**
     * 租户套餐创建 / 更新输入。
     *
     * @param code            套餐编码
     * @param name            套餐名称
     * @param maxUsers        允许的最大用户数
     * @param maxApps         允许的最大应用数
     * @param permissionCodes 套餐包含的权限代码列表
     */

    /**
     * 套餐状态更新输入。
     *
     * @param status 目标状态：active / inactive
     */

    /**
     * 套餐权限替换输入。
     *
     * @param permissionCodes 新的权限代码列表
     */
}
