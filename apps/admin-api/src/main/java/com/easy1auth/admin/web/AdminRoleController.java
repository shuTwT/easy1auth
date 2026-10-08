package com.easy1auth.admin.web;

import com.easy1auth.admin.web.dto.*;
import com.easy1auth.admin.annotation.UserPoolManagementPermission;
import com.easy1auth.system.constant.ManagementPermissionCode;
import com.easy1auth.system.service.AdminAccessService;
import com.easy1auth.framework.web.response.ApiResponse;
import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.framework.tenant.context.UserPoolContextHolder;
import com.easy1auth.tenant.dto.UserPoolAccess;
import com.easy1auth.tenant.service.UserPoolService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员角色管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/admin-roles}，提供用户池内管理员角色
 * （admin_user 角色）的统计、权限目录、分页查询、详情、创建、更新与删除能力。
 * 所有操作均通过 {@link UserPoolManagementPermission} 做用户池级权限控制，并以
 * {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/admin-roles")
public class AdminRoleController {
    /** 管理访问服务（管理员角色与成员） */
    private final AdminAccessService access;
    /** 用户池服务（解析当前账号在用户池内可授予的权限） */
    private final UserPoolService userPools;

    AdminRoleController(AdminAccessService access, UserPoolService userPools) {
        this.access = access;
        this.userPools = userPools;
    }

    /** 查询管理员角色的统计信息。 */
    @UserPoolManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_STATS)
    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.ok(access.roleStats());
    }

    /** 查询可分配给管理员角色的权限目录。 */
    @UserPoolManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_PERMISSIONS_CATALOG)
    @GetMapping("/permissions/catalog")
    public ApiResponse<?> catalog() {
        return ApiResponse.ok(new PermissionCatalogResponse(access.catalog()));
    }

    /** 分页查询管理员角色列表，支持按名称与系统角色过滤。 */
    @UserPoolManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_LIST)
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String name, @RequestParam(required = false) Boolean isSystem) {
        var p = access.roles(page, pageSize, name, isSystem);
        return ApiResponse.ok(p);
    }

    /** 查询指定管理员角色的详情。 */
    @UserPoolManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_READ)
    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(access.role(id));
    }

    /** 创建管理员角色并绑定权限集合。 */
    @UserPoolManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_CREATE)
    @PostMapping
    public ApiResponse<?> create(@AuthenticationPrincipal Jwt principal, @RequestBody RoleInput in) {
        UserPoolAccess tenantAccess = currentAccess(principal);
        return ApiResponse.ok(access.create(
                tenantAccess.userPoolId(),
                tenantAccess.permissions(),
                in.name(),
                in.code(),
                in.description(),
                in.permissions()), "管理员角色创建成功");
    }

    /** 更新指定管理员角色的名称、描述与权限集合。 */
    @UserPoolManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<?> update(@AuthenticationPrincipal Jwt principal, @PathVariable UUID id,
                                 @RequestBody RoleInput in) {
        UserPoolAccess tenantAccess = currentAccess(principal);
        return ApiResponse.ok(access.update(
                tenantAccess.userPoolId(),
                tenantAccess.permissions(),
                id,
                in.name(),
                in.code(),
                in.description(),
                in.permissions()), "管理员角色更新成功");
    }

    /** 删除指定管理员角色。 */
    @UserPoolManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        access.delete(id);
        return ApiResponse.ok(null, "管理员角色删除成功");
    }

    /** 解析当前登录账号在 Holder 指定用户池下的有效访问。 */
    private UserPoolAccess currentAccess(Jwt principal) {
        UUID accountId = UUID.fromString(principal.getSubject());
        UUID userPoolId = UserPoolContextHolder.getUserPoolId();
        if (userPoolId == null) {
            throw new DomainException(com.easy1auth.tenant.constant.ErrorCodeConstants.USER_POOL_CONTEXT_REQUIRED);
        }
        return userPools.authorize(accountId, userPoolId);
    }

    /**
     * 管理员角色输入。
     *
     * @param name        角色名称
     * @param description 角色描述（可选）
     * @param permissions 角色拥有的权限代码列表（可选）
     */

    /**
     * 权限目录响应。
     *
     * @param permissions 可分配的权限目录列表
     */
}
