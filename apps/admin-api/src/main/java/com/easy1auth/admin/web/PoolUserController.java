package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.directory.*;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.useraccess.UserAccessCatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
public class PoolUserController {
    private final PoolUserService users;
    private final DirectoryCatalogService directory;
    private final UserAccessCatalogService access;

    PoolUserController(PoolUserService users, DirectoryCatalogService directory, UserAccessCatalogService access) {
        this.users = users;
        this.directory = directory;
        this.access = access;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_LIST)
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String username, @RequestParam(required = false) String email, @RequestParam(required = false) String phone, @RequestParam(required = false) String name, @RequestParam(required = false) String status, @RequestParam(required = false) String department) {
        var p = users.list(page, pageSize, username, email, phone, name, status, department);
        return ApiResponse.ok(p);
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_STATS)
    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.ok(users.stats());
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_READ)
    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(users.get(id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_CREATE)
    @PostMapping
    public ApiResponse<?> create(@RequestBody PoolUserService.Input in) {
        return ApiResponse.ok(users.create(in), "用户创建成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable UUID id, @RequestBody PoolUserService.Input in) {
        return ApiResponse.ok(users.update(id, in), "用户更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        users.delete(id);
        return ApiResponse.ok(null, "用户删除成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_STATUS)
    @PutMapping("/{id}/status")
    public ApiResponse<?> status(@PathVariable UUID id, @RequestBody StatusInput in) {
        return ApiResponse.ok(users.status(id, in.status()), "状态更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_RESET_PASSWORD)
    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> password(@PathVariable UUID id, @RequestBody PasswordInput in) {
        users.resetPassword(id, in.newPassword());
        return ApiResponse.ok(null, "密码重置成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_CHANGE_PASSWORD)
    @PostMapping("/{id}/change-password")
    public ApiResponse<Void> changePassword(@PathVariable UUID id, @RequestBody ChangePasswordInput in) {
        users.changePassword(id, in.oldPassword(), in.newPassword());
        return ApiResponse.ok(null, "密码修改成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLES_READ)
    @GetMapping("/{id}/roles")
    public ApiResponse<?> roles(@PathVariable UUID id) {
        return ApiResponse.ok(access.rolesForUser(id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLES_ASSIGN)
    @PostMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable UUID id, @RequestBody RoleIds in) {
        access.replaceUserRoles(id, in.roleIds());
        return ApiResponse.ok(null, "角色分配成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_GROUPS_READ)
    @GetMapping("/{id}/groups")
    public ApiResponse<?> groups(@PathVariable UUID id) {
        return ApiResponse.ok(directory.groupsForUser(id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.USER_GROUPS_ASSIGN)
    @PostMapping("/{id}/groups")
    public ApiResponse<Void> assignGroups(@PathVariable UUID id, @RequestBody GroupIds in) {
        directory.replaceUserGroups(id, in.groupIds());
        return ApiResponse.ok(null, "用户组分配成功");
    }

    public record StatusInput(String status) {
    }

    public record PasswordInput(String newPassword) {
    }

    public record ChangePasswordInput(String oldPassword, String newPassword) {
    }

    public record RoleIds(List<UUID> roleIds) {
    }

    public record GroupIds(List<UUID> groupIds) {
    }
}
