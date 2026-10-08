package com.easy1auth.admin.web;

import com.easy1auth.admin.annotation.UserPoolManagementPermission;
import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.framework.common.pagination.PageData;
import com.easy1auth.framework.tenant.context.UserPoolContextHolder;
import com.easy1auth.framework.web.response.ApiResponse;
import com.easy1auth.system.constant.ManagementPermissionCode;
import com.easy1auth.tenant.dto.TenantInput;
import com.easy1auth.tenant.dto.TenantView;
import com.easy1auth.tenant.service.TenantService;
import com.easy1auth.tenant.service.UserPoolService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/** 当前用户池下的租户资料接口。不会设置或改变管理端用户池上下文。 */
@RestController
@RequestMapping("/api/pool-tenants")
public class TenantController {
    private final TenantService tenants;
    private final UserPoolService userPools;

    TenantController(TenantService tenants, UserPoolService userPools) {
        this.tenants = tenants;
        this.userPools = userPools;
    }

    @UserPoolManagementPermission(ManagementPermissionCode.POOL_TENANT_LIST)
    @GetMapping
    public ApiResponse<PageData<TenantView>> list(Principal principal,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int pageSize,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) String status) {
        UUID userPoolId = require(principal, ManagementPermissionCode.POOL_TENANT_LIST);
        return ApiResponse.ok(tenants.list(userPoolId, page, pageSize, name, status));
    }

    @UserPoolManagementPermission(ManagementPermissionCode.POOL_TENANT_READ)
    @GetMapping("/{id}")
    public ApiResponse<TenantView> get(Principal principal, @PathVariable UUID id) {
        return ApiResponse.ok(tenants.get(require(principal, ManagementPermissionCode.POOL_TENANT_READ), id));
    }

    @UserPoolManagementPermission(ManagementPermissionCode.POOL_TENANT_CREATE)
    @PostMapping
    public ApiResponse<TenantView> create(Principal principal, @RequestBody TenantInput input) {
        return ApiResponse.ok(tenants.create(require(principal, ManagementPermissionCode.POOL_TENANT_CREATE), input), "租户创建成功");
    }

    @UserPoolManagementPermission(ManagementPermissionCode.POOL_TENANT_UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<TenantView> update(Principal principal, @PathVariable UUID id, @RequestBody TenantInput input) {
        return ApiResponse.ok(tenants.update(require(principal, ManagementPermissionCode.POOL_TENANT_UPDATE), id, input), "租户更新成功");
    }

    @UserPoolManagementPermission(ManagementPermissionCode.POOL_TENANT_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Principal principal, @PathVariable UUID id) {
        tenants.delete(require(principal, ManagementPermissionCode.POOL_TENANT_DELETE), id);
        return ApiResponse.ok(null, "租户已删除");
    }

    /** 明确校验当前管理员可管理该用户池，不能只信任客户端传入的用户池请求头。 */
    private UUID require(Principal principal, ManagementPermissionCode permission) {
        UUID userPoolId = UserPoolContextHolder.getUserPoolId();
        if (userPoolId == null) {
            throw new DomainException(com.easy1auth.tenant.constant.ErrorCodeConstants.USER_POOL_CONTEXT_REQUIRED);
        }
        var access = userPools.authorize(UUID.fromString(principal.getName()), userPoolId);
        if (!access.permissions().contains(permission.value())) {
            throw new DomainException(com.easy1auth.system.constant.ErrorCodeConstants.PERMISSION_DENIED);
        }
        return userPoolId;
    }
}
