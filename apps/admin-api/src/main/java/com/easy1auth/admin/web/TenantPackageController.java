package com.easy1auth.admin.web;

import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.admin.security.PlatformManagementPermission;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.TenantPackageMutation;
import com.easy1auth.tenant.TenantPackageService;
import com.easy1auth.tenant.TenantPackageView;
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

@RestController
@RequestMapping("/api/platform/tenant-packages")
public class TenantPackageController {
    private final PlatformAuthorizationResolver platformAuthorization;
    private final TenantPackageService packages;

    TenantPackageController(PlatformAuthorizationResolver platformAuthorization, TenantPackageService packages) {
        this.platformAuthorization = platformAuthorization;
        this.packages = packages;
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_LIST)
    @GetMapping
    public ApiResponse<List<TenantPackageView>> list(@AuthenticationPrincipal Jwt actor) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_LIST);
        return ApiResponse.ok(packages.list());
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_READ)
    @GetMapping("/{packageId}")
    public ApiResponse<TenantPackageView> get(@AuthenticationPrincipal Jwt actor, @PathVariable long packageId) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_READ);
        return ApiResponse.ok(packages.get(packageId));
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_CREATE)
    @PostMapping
    public ApiResponse<TenantPackageView> create(@AuthenticationPrincipal Jwt actor, @RequestBody TenantPackageInput input) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_CREATE);
        return ApiResponse.ok(packages.create(mutation(input)), "租户套餐创建成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_UPDATE)
    @PutMapping("/{packageId}")
    public ApiResponse<TenantPackageView> update(@AuthenticationPrincipal Jwt actor, @PathVariable long packageId,
                                                 @RequestBody TenantPackageInput input) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_UPDATE);
        return ApiResponse.ok(packages.update(packageId, mutation(input)), "租户套餐更新成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_STATUS)
    @PutMapping("/{packageId}/status")
    public ApiResponse<TenantPackageView> updateStatus(@AuthenticationPrincipal Jwt actor, @PathVariable long packageId,
                                                       @RequestBody TenantPackageStatusInput input) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_STATUS);
        return ApiResponse.ok(packages.updateStatus(packageId, input == null ? null : input.status()), "租户套餐状态更新成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_PERMISSION_REPLACE)
    @PutMapping("/{packageId}/permissions")
    public ApiResponse<TenantPackageView> replacePermissions(@AuthenticationPrincipal Jwt actor, @PathVariable long packageId,
                                                             @RequestBody TenantPackagePermissionsInput input) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_PERMISSION_REPLACE);
        return ApiResponse.ok(packages.replacePermissions(packageId, input == null ? null : input.permissionCodes()), "租户套餐权限更新成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_DELETE)
    @DeleteMapping("/{packageId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt actor, @PathVariable long packageId) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_DELETE);
        packages.delete(packageId);
        return ApiResponse.ok(null, "租户套餐删除成功");
    }

    private void require(Jwt actor, ManagementPermissionCode permission) {
        platformAuthorization.require(accountId(actor), permission);
    }

    private static UUID accountId(Jwt actor) {
        if (actor == null || actor.getSubject() == null) {
            throw new DomainException("AUTHENTICATION_SUBJECT_INVALID", "认证主体无效", 403);
        }
        try {
            return UUID.fromString(actor.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new DomainException("AUTHENTICATION_SUBJECT_INVALID", "认证主体无效", 403);
        }
    }

    private static TenantPackageMutation mutation(TenantPackageInput input) {
        if (input == null) {
            throw new DomainException("TENANT_PACKAGE_REQUIRED", "租户套餐不能为空", 400);
        }
        return input.toMutation();
    }

    public record TenantPackageInput(
            String code,
            String name,
            int maxUsers,
            int maxApps,
            List<String> permissionCodes
    ) {
        TenantPackageMutation toMutation() {
            return new TenantPackageMutation(code, name, false, maxUsers, maxApps, permissionCodes);
        }
    }

    public record TenantPackageStatusInput(String status) {
    }

    public record TenantPackagePermissionsInput(List<String> permissionCodes) {
    }
}
