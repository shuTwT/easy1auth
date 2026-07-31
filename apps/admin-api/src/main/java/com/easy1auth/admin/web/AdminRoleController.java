package com.easy1auth.admin.web;

import com.easy1auth.tenant.WebFramework;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.*;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
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
    public ApiResponse<?> stats(HttpServletRequest r) {
        return ApiResponse.ok(access.roleStats(context(r)));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_PERMISSIONS_CATALOG)
    @GetMapping("/permissions/catalog")
    public ApiResponse<?> catalog(HttpServletRequest r) {
        return ApiResponse.ok(Map.of("permissions", access.catalog(context(r))));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_LIST)
    @GetMapping
    public ApiResponse<?> list(HttpServletRequest r, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String name, @RequestParam(required = false) Boolean isSystem) {
        var p = access.roles(context(r), page, pageSize, name, isSystem);
        return ApiResponse.ok(PageData.of(p.roles(), p.page(), p.pageSize(), p.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_READ)
    @GetMapping("/{id}")
    public ApiResponse<?> get(HttpServletRequest r, @PathVariable UUID id) {
        return ApiResponse.ok(access.role(context(r), id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_CREATE)
    @PostMapping
    public ApiResponse<?> create(HttpServletRequest r, @RequestBody RoleInput in) {
        return ApiResponse.ok(access.create(context(r), in.name(), in.description(), in.permissions()), "管理员角色创建成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<?> update(HttpServletRequest r, @PathVariable UUID id, @RequestBody RoleInput in) {
        return ApiResponse.ok(access.update(context(r), id, in.name(), in.description(), in.permissions()), "管理员角色更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.ADMIN_ROLE_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(HttpServletRequest r, @PathVariable UUID id) {
        access.delete(context(r), id);
        return ApiResponse.ok(null, "管理员角色删除成功");
    }

    private static TenantContext context(HttpServletRequest r) {
        return WebFramework.requireTenantContext(r);
    }

    public record RoleInput(String name, String description, List<String> permissions) {
    }
}
