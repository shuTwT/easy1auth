package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.directory.*;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.useraccess.UserAccessCatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 目录用户（pool_user）管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/users}，提供目录用户的分页查询、统计、
 * 增删改、启停、密码重置 / 修改，以及角色与用户组分配能力。所有操作均通过
 * {@link TenantManagementPermission} 做租户级权限控制，并以 {@link ApiResponse}
 * 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/users")
public class PoolUserController {
    /** 目录用户服务 */
    private final PoolUserService users;
    /** 组织架构目录服务（用户组分配） */
    private final DirectoryCatalogService directory;
    /** 用户访问目录服务（角色分配） */
    private final UserAccessCatalogService access;

    PoolUserController(PoolUserService users, DirectoryCatalogService directory, UserAccessCatalogService access) {
        this.users = users;
        this.directory = directory;
        this.access = access;
    }

    /** 分页查询目录用户列表，支持按用户名、邮箱、手机号、姓名、状态、部门过滤。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_LIST)
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String username, @RequestParam(required = false) String email, @RequestParam(required = false) String phone, @RequestParam(required = false) String name, @RequestParam(required = false) String status, @RequestParam(required = false) String department) {
        var p = users.list(page, pageSize, username, email, phone, name, status, department);
        return ApiResponse.ok(p);
    }

    /** 查询目录用户的统计信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_STATS)
    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.ok(users.stats());
    }

    /** 查询指定目录用户的详情。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_READ)
    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(users.get(id));
    }

    /** 创建目录用户。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_CREATE)
    @PostMapping
    public ApiResponse<?> create(@RequestBody PoolUserService.Input in) {
        return ApiResponse.ok(users.create(in), "用户创建成功");
    }

    /** 更新指定目录用户的信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable UUID id, @RequestBody PoolUserService.Input in) {
        return ApiResponse.ok(users.update(id, in), "用户更新成功");
    }

    /** 删除指定目录用户。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        users.delete(id);
        return ApiResponse.ok(null, "用户删除成功");
    }

    /** 更新指定目录用户的启停状态。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_STATUS)
    @PutMapping("/{id}/status")
    public ApiResponse<?> status(@PathVariable UUID id, @RequestBody StatusInput in) {
        return ApiResponse.ok(users.status(id, in.status()), "状态更新成功");
    }

    /** 重置指定目录用户的登录密码。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_RESET_PASSWORD)
    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> password(@PathVariable UUID id, @RequestBody PasswordInput in) {
        users.resetPassword(id, in.newPassword());
        return ApiResponse.ok(null, "密码重置成功");
    }

    /** 修改指定目录用户的密码（需校验原密码）。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_CHANGE_PASSWORD)
    @PostMapping("/{id}/change-password")
    public ApiResponse<Void> changePassword(@PathVariable UUID id, @RequestBody ChangePasswordInput in) {
        users.changePassword(id, in.oldPassword(), in.newPassword());
        return ApiResponse.ok(null, "密码修改成功");
    }

    /** 查询指定目录用户已分配的角色。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLES_READ)
    @GetMapping("/{id}/roles")
    public ApiResponse<?> roles(@PathVariable UUID id) {
        return ApiResponse.ok(access.rolesForUser(id));
    }

    /** 全量替换指定目录用户的角色分配。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_ROLES_ASSIGN)
    @PostMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable UUID id, @RequestBody RoleIds in) {
        access.replaceUserRoles(id, in.roleIds());
        return ApiResponse.ok(null, "角色分配成功");
    }

    /** 查询指定目录用户所属的用户组。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_GROUPS_READ)
    @GetMapping("/{id}/groups")
    public ApiResponse<?> groups(@PathVariable UUID id) {
        return ApiResponse.ok(directory.groupsForUser(id));
    }

    /** 全量替换指定目录用户的用户组分配。 */
    @TenantManagementPermission(value = ManagementPermissionCode.USER_GROUPS_ASSIGN)
    @PostMapping("/{id}/groups")
    public ApiResponse<Void> assignGroups(@PathVariable UUID id, @RequestBody GroupIds in) {
        directory.replaceUserGroups(id, in.groupIds());
        return ApiResponse.ok(null, "用户组分配成功");
    }

    /**
     * 用户状态输入。
     *
     * @param status 目标状态：active / disabled
     */
    public record StatusInput(String status) {
    }

    /**
     * 密码重置输入。
     *
     * @param newPassword 新密码
     */
    public record PasswordInput(String newPassword) {
    }

    /**
     * 密码修改输入。
     *
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    public record ChangePasswordInput(String oldPassword, String newPassword) {
    }

    /**
     * 角色 ID 集合。
     *
     * @param roleIds 待分配的角色 ID 列表
     */
    public record RoleIds(List<UUID> roleIds) {
    }

    /**
     * 用户组 ID 集合。
     *
     * @param groupIds 待分配的用户组 ID 列表
     */
    public record GroupIds(List<UUID> groupIds) {
    }
}
