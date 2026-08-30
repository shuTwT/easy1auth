package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import com.easy1auth.poolidentity.dto.PermissionInput;
import com.easy1auth.poolidentity.dto.RoleInput;
import com.easy1auth.poolidentity.service.UserAccessCatalogService;
import com.easy1auth.tenant.util.TenantContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 用户访问目录接口（角色与数据权限）。
 *
 * <p>管理端 REST 入口，提供目录用户角色（{@code /api/roles}）与数据权限
 * （{@code /api/permissions}）的分页查询、统计、树形结构、增删改以及用户与角色
 * 的关联管理能力。所有操作均通过 {@link TenantManagementPermission} 做租户级
 * 权限控制，并以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
public class UserAccessCatalogController {
    /** 用户访问目录服务 */
    private final UserAccessCatalogService service;

    UserAccessCatalogController(UserAccessCatalogService service) {
        this.service = service;
    }

    /** 分页查询目录用户角色列表，支持按关键字搜索与类型过滤。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_LIST)
    @GetMapping("/api/roles")
    ApiResponse<?> roles(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String type) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        var p = service.roles(tenantId,page, pageSize, search, type);
        return ApiResponse.ok(p);
    }

    /** 查询目录用户角色的统计信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_STATS)
    @GetMapping("/api/roles/stats")
    ApiResponse<?> roleStats() {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return ApiResponse.ok(service.roleStats(tenantId));
    }

    /** 查询目录用户角色的树形结构。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_TREE)
    @GetMapping("/api/roles/tree")
    ApiResponse<?> roleTree() {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return ApiResponse.ok(service.roleTree(tenantId));
    }

    /** 查询指定目录用户角色的详情。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_READ)
    @GetMapping("/api/roles/{id}")
    ApiResponse<?> role(@PathVariable UUID id) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return ApiResponse.ok(service.role(tenantId,id));
    }

    /** 创建目录用户角色。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_CREATE)
    @PostMapping("/api/roles")
    ApiResponse<?> createRole(@RequestBody RoleInput in) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return ApiResponse.ok(service.createRole(tenantId,in), "角色创建成功");
    }

    /** 更新指定目录用户角色的信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_UPDATE)
    @PutMapping("/api/roles/{id}")
    ApiResponse<?> updateRole(@PathVariable UUID id, @RequestBody RoleInput in) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return ApiResponse.ok(service.updateRole(tenantId,id, in), "角色更新成功");
    }

    /** 删除指定目录用户角色。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_DELETE)
    @DeleteMapping("/api/roles/{id}")
    ApiResponse<Void> deleteRole(@PathVariable UUID id) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        service.deleteRole(tenantId,id);
        return ApiResponse.ok(null, "角色删除成功");
    }

    /** 查询指定角色下的用户列表。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USERS_READ)
    @GetMapping("/api/roles/{id}/users")
    ApiResponse<?> roleUsers(@PathVariable UUID id, @RequestParam(required = false) String search) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return ApiResponse.ok(service.roleUsers(tenantId,id, search));
    }

    /** 向指定角色批量分配用户。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USERS_ASSIGN)
    @PostMapping("/api/roles/{id}/users")
    ApiResponse<Void> assignUsers(@PathVariable UUID id, @RequestBody UserIds in) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        service.assignUsers(id, in.userIds());
        return ApiResponse.ok(null, "分配用户成功");
    }

    /** 从指定角色批量移除用户。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USERS_REMOVE)
    @DeleteMapping("/api/roles/{id}/users")
    ApiResponse<Void> removeUsers(@PathVariable UUID id, @RequestBody UserIds in) {
        service.removeUsers(id, in.userIds());
        return ApiResponse.ok(null, "移除用户成功");
    }

    /** 查询指定用户的全部角色。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USER_ROLES_READ)
    @GetMapping("/api/roles/user/{userId}")
    ApiResponse<?> userRoles(@PathVariable UUID userId) {
        return ApiResponse.ok(service.rolesForUser(userId));
    }

    /** 全量替换指定用户的角色分配。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLE_USER_ROLES_REPLACE)
    @PostMapping("/api/roles/user/{userId}")
    ApiResponse<Void> replaceRoles(@PathVariable UUID userId, @RequestBody RoleIds in) {
        service.replaceUserRoles(userId, in.roleIds());
        return ApiResponse.ok(null, "分配角色成功");
    }

    /** 分页查询数据权限列表，支持按关键字、类型与资源过滤。 */
    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_LIST)
    @GetMapping("/api/permissions")
    ApiResponse<?> permissions(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String type, @RequestParam(required = false) String resource) {
        var p = service.permissions(page, pageSize, search, type, resource);
        return ApiResponse.ok(p);
    }

    /** 查询数据权限的统计信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_STATS)
    @GetMapping("/api/permissions/stats")
    ApiResponse<?> permissionStats() {
        return ApiResponse.ok(service.permissionStats());
    }

    /** 查询数据权限的树形结构。 */
    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_TREE)
    @GetMapping("/api/permissions/tree")
    ApiResponse<?> permissionTree() {
        return ApiResponse.ok(service.permissionTree());
    }

    /** 查询指定数据权限的详情。 */
    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_READ)
    @GetMapping("/api/permissions/{id}")
    ApiResponse<?> permission(@PathVariable UUID id) {
        return ApiResponse.ok(service.permission(id));
    }

    /** 创建数据权限。 */
    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_CREATE)
    @PostMapping("/api/permissions")
    ApiResponse<?> createPermission(@RequestBody PermissionInput in) {
        return ApiResponse.ok(service.createPermission(in), "创建权限成功");
    }

    /** 更新指定数据权限。 */
    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_UPDATE)
    @PutMapping("/api/permissions/{id}")
    ApiResponse<?> updatePermission(@PathVariable UUID id, @RequestBody PermissionInput in) {
        return ApiResponse.ok(service.updatePermission(id, in), "更新权限成功");
    }

    /** 删除指定数据权限。 */
    @TenantManagementPermission(value = ManagementPermissionCode.DATA_PERMISSION_DELETE)
    @DeleteMapping("/api/permissions/{id}")
    ApiResponse<Void> deletePermission(@PathVariable UUID id) {
        service.deletePermission(id);
        return ApiResponse.ok(null, "删除权限成功");
    }

    /**
     * 用户 ID 集合请求体。
     *
     * @param userIds 用户 ID 列表，为空时按空列表处理
     */
    public record UserIds(List<UUID> userIds) {
        public UserIds {
            userIds = userIds == null ? List.of() : List.copyOf(userIds);
        }
    }

    /**
     * 角色 ID 集合请求体。
     *
     * @param roleIds 角色 ID 列表，为空时按空列表处理
     */
    public record RoleIds(List<UUID> roleIds) {
        public RoleIds {
            roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
        }
    }
}
