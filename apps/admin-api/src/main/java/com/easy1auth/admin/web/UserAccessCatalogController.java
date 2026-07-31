package com.easy1auth.admin.web;

import com.easy1auth.tenant.WebFramework;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.tenant.TenantContext;
import com.easy1auth.useraccess.UserAccessCatalogService;
import jakarta.servlet.http.HttpServletRequest;
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
    ApiResponse<?> roles(HttpServletRequest r, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String type) {
        var p = service.roles(c(r).tenantId(), page, pageSize, search, type);
        return ApiResponse.ok(PageData.of(p.roles(), p.page(), p.pageSize(), p.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_STATS)
    @GetMapping("/api/roles/stats")
    ApiResponse<?> roleStats(HttpServletRequest r) {
        return ApiResponse.ok(service.roleStats(c(r).tenantId()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_TREE)
    @GetMapping("/api/roles/tree")
    ApiResponse<?> roleTree(HttpServletRequest r) {
        return ApiResponse.ok(service.roleTree(c(r).tenantId()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_READ)
    @GetMapping("/api/roles/{id}")
    ApiResponse<?> role(HttpServletRequest r, @PathVariable UUID id) {
        return ApiResponse.ok(service.role(c(r).tenantId(), id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_CREATE)
    @PostMapping("/api/roles")
    ApiResponse<?> createRole(HttpServletRequest r, @RequestBody UserAccessCatalogService.RoleInput in) {
        return ApiResponse.ok(service.createRole(c(r).tenantId(), in), "角色创建成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_UPDATE)
    @PutMapping("/api/roles/{id}")
    ApiResponse<?> updateRole(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserAccessCatalogService.RoleInput in) {
        return ApiResponse.ok(service.updateRole(c(r).tenantId(), id, in), "角色更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_DELETE)
    @DeleteMapping("/api/roles/{id}")
    ApiResponse<Void> deleteRole(HttpServletRequest r, @PathVariable UUID id) {
        service.deleteRole(c(r).tenantId(), id);
        return ApiResponse.ok(null, "角色删除成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USERS_READ)
    @GetMapping("/api/roles/{id}/users")
    ApiResponse<?> roleUsers(HttpServletRequest r, @PathVariable UUID id, @RequestParam(required = false) String search) {
        return ApiResponse.ok(service.roleUsers(c(r).tenantId(), id, search));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USERS_ASSIGN)
    @PostMapping("/api/roles/{id}/users")
    ApiResponse<Void> assignUsers(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) {
        service.assignUsers(c(r).tenantId(), id, in.userIds());
        return ApiResponse.ok(null, "分配用户成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USERS_REMOVE)
    @DeleteMapping("/api/roles/{id}/users")
    ApiResponse<Void> removeUsers(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) {
        service.removeUsers(c(r).tenantId(), id, in.userIds());
        return ApiResponse.ok(null, "移除用户成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USER_ROLES_READ)
    @GetMapping("/api/roles/user/{userId}")
    ApiResponse<?> userRoles(HttpServletRequest r, @PathVariable UUID userId) {
        return ApiResponse.ok(service.rolesForUser(c(r).tenantId(), userId));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USER_ROLES_REPLACE)
    @PostMapping("/api/roles/user/{userId}")
    ApiResponse<Void> replaceRoles(HttpServletRequest r, @PathVariable UUID userId, @RequestBody RoleIds in) {
        service.replaceUserRoles(c(r).tenantId(), userId, in.roleIds());
        return ApiResponse.ok(null, "分配角色成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_LIST)
    @GetMapping("/api/permissions")
    ApiResponse<?> permissions(HttpServletRequest r, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String type, @RequestParam(required = false) String resource) {
        var p = service.permissions(c(r).tenantId(), page, pageSize, search, type, resource);
        return ApiResponse.ok(PageData.of(p.permissions(), p.page(), p.pageSize(), p.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_STATS)
    @GetMapping("/api/permissions/stats")
    ApiResponse<?> permissionStats(HttpServletRequest r) {
        return ApiResponse.ok(service.permissionStats(c(r).tenantId()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_TREE)
    @GetMapping("/api/permissions/tree")
    ApiResponse<?> permissionTree(HttpServletRequest r) {
        return ApiResponse.ok(service.permissionTree(c(r).tenantId()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_READ)
    @GetMapping("/api/permissions/{id}")
    ApiResponse<?> permission(HttpServletRequest r, @PathVariable UUID id) {
        return ApiResponse.ok(service.permission(c(r).tenantId(), id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_CREATE)
    @PostMapping("/api/permissions")
    ApiResponse<?> createPermission(HttpServletRequest r, @RequestBody UserAccessCatalogService.PermissionInput in) {
        return ApiResponse.ok(service.createPermission(c(r).tenantId(), in), "创建权限成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_UPDATE)
    @PutMapping("/api/permissions/{id}")
    ApiResponse<?> updatePermission(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserAccessCatalogService.PermissionInput in) {
        return ApiResponse.ok(service.updatePermission(c(r).tenantId(), id, in), "更新权限成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_DELETE)
    @DeleteMapping("/api/permissions/{id}")
    ApiResponse<Void> deletePermission(HttpServletRequest r, @PathVariable UUID id) {
        service.deletePermission(c(r).tenantId(), id);
        return ApiResponse.ok(null, "删除权限成功");
    }

    private static TenantContext c(HttpServletRequest r) {
        return WebFramework.requireTenantContext(r);
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
