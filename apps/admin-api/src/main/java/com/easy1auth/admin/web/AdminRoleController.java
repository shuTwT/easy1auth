package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.*;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import com.easy1auth.tenant.TenantContext;
import com.easy1auth.tenant.WebFramework;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员角色管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/admin-roles}，提供租户内管理员角色
 * （admin_user 角色）的统计、权限目录、分页查询、详情、创建、更新与删除能力。
 * 所有操作均通过 {@link TenantManagementPermission} 做租户级权限控制，并以
 * {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/admin-roles")
public class AdminRoleController {
    /** 管理访问服务（管理员角色与成员） */
    private final AdminAccessService access;

    AdminRoleController(AdminAccessService access) {
        this.access = access;
    }

    /** 查询管理员角色的统计信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_STATS)
    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.ok(access.roleStats());
    }

    /** 查询可分配给管理员角色的权限目录。 */
    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_PERMISSIONS_CATALOG)
    @GetMapping("/permissions/catalog")
    public ApiResponse<?> catalog() {
        return ApiResponse.ok(new PermissionCatalogResponse(access.catalog()));
    }

    /** 分页查询管理员角色列表，支持按名称与系统角色过滤。 */
    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_LIST)
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String name, @RequestParam(required = false) Boolean isSystem) {
        var p = access.roles(page, pageSize, name, isSystem);
        return ApiResponse.ok(p);
    }

    /** 查询指定管理员角色的详情。 */
    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_READ)
    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(access.role(id));
    }

    /** 创建管理员角色并绑定权限集合。 */
    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_CREATE)
    @PostMapping
    public ApiResponse<?> create(@RequestAttribute(WebFramework.TENANT_CONTEXT_ATTRIBUTE) TenantContext context, @RequestBody RoleInput in) {
        return ApiResponse.ok(access.create(context, in.name(), in.description(), in.permissions()), "管理员角色创建成功");
    }

    /** 更新指定管理员角色的名称、描述与权限集合。 */
    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<?> update(@RequestAttribute(WebFramework.TENANT_CONTEXT_ATTRIBUTE) TenantContext context, @PathVariable UUID id, @RequestBody RoleInput in) {
        return ApiResponse.ok(access.update(context, id, in.name(), in.description(), in.permissions()), "管理员角色更新成功");
    }

    /** 删除指定管理员角色。 */
    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        access.delete(id);
        return ApiResponse.ok(null, "管理员角色删除成功");
    }

    /**
     * 管理员角色输入。
     *
     * @param name        角色名称
     * @param description 角色描述（可选）
     * @param permissions 角色拥有的权限代码列表（可选）
     */
    public record RoleInput(String name, String description, List<String> permissions) {
    }

    /**
     * 权限目录响应。
     *
     * @param permissions 可分配的权限目录列表
     */
    public record PermissionCatalogResponse(List<?> permissions) {
    }
}
