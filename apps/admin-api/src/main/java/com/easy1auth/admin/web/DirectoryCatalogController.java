package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantContextFilter;
import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.directory.DirectoryCatalogService;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.tenant.TenantContext;
import com.easy1auth.tenant.TenantDataBoundary;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class DirectoryCatalogController {
    private final DirectoryCatalogService catalog;
    DirectoryCatalogController(DirectoryCatalogService catalog) { this.catalog = catalog; }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_LIST, boundary = TenantDataBoundary.TENANT_ALL)
    @GetMapping("/api/groups")
    ApiResponse<?> groups(HttpServletRequest r, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize,
                          @RequestParam(required=false) String name, @RequestParam(required=false) String type, @RequestParam(required=false) UUID parentId) {
        var x = catalog.groups(c(r).tenantId(), page, pageSize, name, type, parentId);
        return ApiResponse.ok(PageData.of(x.data(), x.page(), x.pageSize(), x.total()));
    }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_TREE, boundary = TenantDataBoundary.TENANT_ALL) @GetMapping("/api/groups/tree") ApiResponse<?> groupTree(HttpServletRequest r) { return ApiResponse.ok(catalog.groupTree(c(r).tenantId())); }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_STATS, boundary = TenantDataBoundary.TENANT_ALL) @GetMapping("/api/groups/stats") ApiResponse<?> groupStats(HttpServletRequest r) { return ApiResponse.ok(catalog.groupStats(c(r).tenantId())); }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_READ, boundary = TenantDataBoundary.TENANT_ALL) @GetMapping("/api/groups/{id}") ApiResponse<?> group(HttpServletRequest r, @PathVariable UUID id) { return ApiResponse.ok(catalog.group(c(r).tenantId(), id)); }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_CREATE, boundary = TenantDataBoundary.TENANT_ALL) @PostMapping("/api/groups") ApiResponse<?> createGroup(HttpServletRequest r, @RequestBody DirectoryCatalogService.GroupInput in) { return ApiResponse.ok(catalog.createGroup(c(r).tenantId(), in), "用户组创建成功"); }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_UPDATE, boundary = TenantDataBoundary.TENANT_ALL) @PutMapping("/api/groups/{id}") ApiResponse<?> updateGroup(HttpServletRequest r, @PathVariable UUID id, @RequestBody DirectoryCatalogService.GroupInput in) { return ApiResponse.ok(catalog.updateGroup(c(r).tenantId(), id, in), "用户组更新成功"); }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_DELETE, boundary = TenantDataBoundary.TENANT_ALL) @DeleteMapping("/api/groups/{id}") ApiResponse<Void> deleteGroup(HttpServletRequest r, @PathVariable UUID id) { catalog.deleteGroup(c(r).tenantId(), id); return ApiResponse.ok(null, "用户组删除成功"); }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_MEMBERS_READ, boundary = TenantDataBoundary.TENANT_ALL) @GetMapping("/api/groups/{id}/members") ApiResponse<?> groupMembers(HttpServletRequest r, @PathVariable UUID id) { return ApiResponse.ok(catalog.groupMembers(c(r).tenantId(), id)); }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_MEMBERS_ADD, boundary = TenantDataBoundary.TENANT_ALL) @PostMapping("/api/groups/{id}/members") ApiResponse<Void> addMembers(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) { catalog.addMembers(c(r).tenantId(), id, in.userIds()); return ApiResponse.ok(null, "成员添加成功"); }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_MEMBERS_REMOVE, boundary = TenantDataBoundary.TENANT_ALL) @DeleteMapping("/api/groups/{id}/members") ApiResponse<Void> removeMembers(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) { catalog.removeMembers(c(r).tenantId(), id, in.userIds()); return ApiResponse.ok(null, "成员移除成功"); }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_ADMIN_ADD, boundary = TenantDataBoundary.TENANT_ALL) @PostMapping("/api/groups/{id}/admins") ApiResponse<Void> addAdmins(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) { catalog.addAdmins(c(r).tenantId(), id, in.userIds()); return ApiResponse.ok(null, "管理员添加成功"); }
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_ADMIN_REMOVE, boundary = TenantDataBoundary.TENANT_ALL) @DeleteMapping("/api/groups/{id}/admins") ApiResponse<Void> removeAdmins(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) { catalog.removeAdmins(c(r).tenantId(), id, in.userIds()); return ApiResponse.ok(null, "管理员移除成功"); }

    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_LIST, boundary = TenantDataBoundary.TENANT_ALL)
    @GetMapping("/api/positions")
    ApiResponse<?> positions(HttpServletRequest r, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize,
                             @RequestParam(required=false) String name, @RequestParam(required=false) String code,
                             @RequestParam(required=false) UUID departmentId, @RequestParam(required=false) Integer level) {
        var x = catalog.positions(c(r).tenantId(), page, pageSize, name, code, departmentId, level);
        return ApiResponse.ok(PageData.of(x.data(), x.page(), x.pageSize(), x.total()));
    }
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_STATS, boundary = TenantDataBoundary.TENANT_ALL) @GetMapping("/api/positions/stats") ApiResponse<?> positionStats(HttpServletRequest r) { return ApiResponse.ok(catalog.positionStats(c(r).tenantId())); }
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_READ, boundary = TenantDataBoundary.TENANT_ALL) @GetMapping("/api/positions/{id}") ApiResponse<?> position(HttpServletRequest r, @PathVariable UUID id) { return ApiResponse.ok(catalog.position(c(r).tenantId(), id)); }
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_CREATE, boundary = TenantDataBoundary.TENANT_ALL) @PostMapping("/api/positions") ApiResponse<?> createPosition(HttpServletRequest r, @RequestBody DirectoryCatalogService.PositionInput in) { return ApiResponse.ok(catalog.createPosition(c(r).tenantId(), in), "岗位创建成功"); }
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_UPDATE, boundary = TenantDataBoundary.TENANT_ALL) @PutMapping("/api/positions/{id}") ApiResponse<?> updatePosition(HttpServletRequest r, @PathVariable UUID id, @RequestBody DirectoryCatalogService.PositionInput in) { return ApiResponse.ok(catalog.updatePosition(c(r).tenantId(), id, in), "岗位更新成功"); }
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_DELETE, boundary = TenantDataBoundary.TENANT_ALL) @DeleteMapping("/api/positions/{id}") ApiResponse<Void> deletePosition(HttpServletRequest r, @PathVariable UUID id) { catalog.deletePosition(c(r).tenantId(), id); return ApiResponse.ok(null, "岗位删除成功"); }

    private static TenantContext c(HttpServletRequest r) { return (TenantContext) Objects.requireNonNull(r.getAttribute(TenantContextFilter.ATTRIBUTE)); }
    public record UserIds(List<UUID> userIds) { public UserIds { userIds = userIds == null ? List.of() : List.copyOf(userIds); } }
}
