package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.*;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.TenantContext;
import com.easy1auth.tenant.WebFramework;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin-roles")
public class AdminRoleController {
    private final AdminAccessService access;

    AdminRoleController(AdminAccessService access) {
        this.access = access;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_STATS)
    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.ok(access.roleStats());
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_PERMISSIONS_CATALOG)
    @GetMapping("/permissions/catalog")
    public ApiResponse<?> catalog() {
        return ApiResponse.ok(new PermissionCatalogResponse(access.catalog()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_LIST)
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String name, @RequestParam(required = false) Boolean isSystem) {
        var p = access.roles(page, pageSize, name, isSystem);
        return ApiResponse.ok(p);
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_READ)
    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(access.role(id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_CREATE)
    @PostMapping
    public ApiResponse<?> create(@RequestAttribute(WebFramework.TENANT_CONTEXT_ATTRIBUTE) TenantContext context, @RequestBody RoleInput in) {
        return ApiResponse.ok(access.create(context, in.name(), in.description(), in.permissions()), "管理员角色创建成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<?> update(@RequestAttribute(WebFramework.TENANT_CONTEXT_ATTRIBUTE) TenantContext context, @PathVariable UUID id, @RequestBody RoleInput in) {
        return ApiResponse.ok(access.update(context, id, in.name(), in.description(), in.permissions()), "管理员角色更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        access.delete(id);
        return ApiResponse.ok(null, "管理员角色删除成功");
    }

    public record RoleInput(String name, String description, List<String> permissions) {
    }

    public record PermissionCatalogResponse(List<?> permissions) {
    }
}
