package com.easy1auth.admin.web;

import com.easy1auth.tenant.TenantContextHolder;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.useraccess.UserAccessCatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class UserAccessCatalogController {
    private final UserAccessCatalogService service;

    UserAccessCatalogController(UserAccessCatalogService service) {
        this.service = service;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_LIST)
    @GetMapping("/api/roles")
    ApiResponse<?> roles(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String type) {
        var p = service.roles(TenantContextHolder.requireTenantId(), page, pageSize, search, type);
        return ApiResponse.ok(PageData.of(p.roles(), p.page(), p.pageSize(), p.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_STATS)
    @GetMapping("/api/roles/stats")
    ApiResponse<?> roleStats() {
        return ApiResponse.ok(service.roleStats(TenantContextHolder.requireTenantId()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_TREE)
    @GetMapping("/api/roles/tree")
    ApiResponse<?> roleTree() {
        return ApiResponse.ok(service.roleTree(TenantContextHolder.requireTenantId()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_READ)
    @GetMapping("/api/roles/{id}")
    ApiResponse<?> role(@PathVariable UUID id) {
        return ApiResponse.ok(service.role(TenantContextHolder.requireTenantId(), id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_CREATE)
    @PostMapping("/api/roles")
    ApiResponse<?> createRole(@RequestBody UserAccessCatalogService.RoleInput in) {
        return ApiResponse.ok(service.createRole(TenantContextHolder.requireTenantId(), in), "角色创建成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_UPDATE)
    @PutMapping("/api/roles/{id}")
    ApiResponse<?> updateRole(@PathVariable UUID id, @RequestBody UserAccessCatalogService.RoleInput in) {
        return ApiResponse.ok(service.updateRole(TenantContextHolder.requireTenantId(), id, in), "角色更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_DELETE)
    @DeleteMapping("/api/roles/{id}")
    ApiResponse<Void> deleteRole(@PathVariable UUID id) {
        service.deleteRole(TenantContextHolder.requireTenantId(), id);
        return ApiResponse.ok(null, "角色删除成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USERS_READ)
    @GetMapping("/api/roles/{id}/users")
    ApiResponse<?> roleUsers(@PathVariable UUID id, @RequestParam(required = false) String search) {
        return ApiResponse.ok(service.roleUsers(TenantContextHolder.requireTenantId(), id, search));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USERS_ASSIGN)
    @PostMapping("/api/roles/{id}/users")
    ApiResponse<Void> assignUsers(@PathVariable UUID id, @RequestBody UserIds in) {
        service.assignUsers(TenantContextHolder.requireTenantId(), id, in.userIds());
        return ApiResponse.ok(null, "分配用户成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USERS_REMOVE)
    @DeleteMapping("/api/roles/{id}/users")
    ApiResponse<Void> removeUsers(@PathVariable UUID id, @RequestBody UserIds in) {
        service.removeUsers(TenantContextHolder.requireTenantId(), id, in.userIds());
        return ApiResponse.ok(null, "移除用户成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USER_ROLES_READ)
    @GetMapping("/api/roles/user/{userId}")
    ApiResponse<?> userRoles(@PathVariable UUID userId) {
        return ApiResponse.ok(service.rolesForUser(TenantContextHolder.requireTenantId(), userId));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USER_ROLES_REPLACE)
    @PostMapping("/api/roles/user/{userId}")
    ApiResponse<Void> replaceRoles(@PathVariable UUID userId, @RequestBody RoleIds in) {
        service.replaceUserRoles(TenantContextHolder.requireTenantId(), userId, in.roleIds());
        return ApiResponse.ok(null, "分配角色成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_LIST)
    @GetMapping("/api/permissions")
    ApiResponse<?> permissions(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String type, @RequestParam(required = false) String resource) {
        var p = service.permissions(TenantContextHolder.requireTenantId(), page, pageSize, search, type, resource);
        return ApiResponse.ok(PageData.of(p.permissions(), p.page(), p.pageSize(), p.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_STATS)
    @GetMapping("/api/permissions/stats")
    ApiResponse<?> permissionStats() {
        return ApiResponse.ok(service.permissionStats(TenantContextHolder.requireTenantId()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_TREE)
    @GetMapping("/api/permissions/tree")
    ApiResponse<?> permissionTree() {
        return ApiResponse.ok(service.permissionTree(TenantContextHolder.requireTenantId()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_READ)
    @GetMapping("/api/permissions/{id}")
    ApiResponse<?> permission(@PathVariable UUID id) {
        return ApiResponse.ok(service.permission(TenantContextHolder.requireTenantId(), id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_CREATE)
    @PostMapping("/api/permissions")
    ApiResponse<?> createPermission(@RequestBody UserAccessCatalogService.PermissionInput in) {
        return ApiResponse.ok(service.createPermission(TenantContextHolder.requireTenantId(), in), "创建权限成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_UPDATE)
    @PutMapping("/api/permissions/{id}")
    ApiResponse<?> updatePermission(@PathVariable UUID id, @RequestBody UserAccessCatalogService.PermissionInput in) {
        return ApiResponse.ok(service.updatePermission(TenantContextHolder.requireTenantId(), id, in), "更新权限成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_DELETE)
    @DeleteMapping("/api/permissions/{id}")
    ApiResponse<Void> deletePermission(@PathVariable UUID id) {
        service.deletePermission(TenantContextHolder.requireTenantId(), id);
        return ApiResponse.ok(null, "删除权限成功");
    }

    public record UserIds(List<UUID> userIds) {
        public UserIds {
            userIds = userIds == null ? List.of() : List.copyOf(userIds);
        }
    }

    public record RoleIds(List<UUID> roleIds) {
        public RoleIds {
            roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
        }
    }
}
