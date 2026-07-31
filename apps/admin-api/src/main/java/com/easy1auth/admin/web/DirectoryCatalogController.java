package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.directory.DirectoryCatalogService;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class DirectoryCatalogController {
    private final DirectoryCatalogService catalog;

    DirectoryCatalogController(DirectoryCatalogService catalog) {
        this.catalog = catalog;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_LIST)
    @GetMapping("/api/groups")
    ApiResponse<?> groups(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String name, @RequestParam(required = false) String type, @RequestParam(required = false) UUID parentId) {
        var x = catalog.groups(page, pageSize, name, type, parentId);
        return ApiResponse.ok(PageData.of(x.data(), x.page(), x.pageSize(), x.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_TREE)
    @GetMapping("/api/groups/tree")
    ApiResponse<?> groupTree() {
        return ApiResponse.ok(catalog.groupTree());
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_STATS)
    @GetMapping("/api/groups/stats")
    ApiResponse<?> groupStats() {
        return ApiResponse.ok(catalog.groupStats());
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_READ)
    @GetMapping("/api/groups/{id}")
    ApiResponse<?> group(@PathVariable UUID id) {
        return ApiResponse.ok(catalog.group(id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_CREATE)
    @PostMapping("/api/groups")
    ApiResponse<?> createGroup(@RequestBody DirectoryCatalogService.GroupInput in) {
        return ApiResponse.ok(catalog.createGroup(in), "用户组创建成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_UPDATE)
    @PutMapping("/api/groups/{id}")
    ApiResponse<?> updateGroup(@PathVariable UUID id, @RequestBody DirectoryCatalogService.GroupInput in) {
        return ApiResponse.ok(catalog.updateGroup(id, in), "用户组更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_DELETE)
    @DeleteMapping("/api/groups/{id}")
    ApiResponse<Void> deleteGroup(@PathVariable UUID id) {
        catalog.deleteGroup(id);
        return ApiResponse.ok(null, "用户组删除成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_MEMBERS_READ)
    @GetMapping("/api/groups/{id}/members")
    ApiResponse<?> groupMembers(@PathVariable UUID id) {
        return ApiResponse.ok(catalog.groupMembers(id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_MEMBERS_ADD)
    @PostMapping("/api/groups/{id}/members")
    ApiResponse<Void> addMembers(@PathVariable UUID id, @RequestBody UserIds in) {
        catalog.addMembers(id, in.userIds());
        return ApiResponse.ok(null, "成员添加成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_MEMBERS_REMOVE)
    @DeleteMapping("/api/groups/{id}/members")
    ApiResponse<Void> removeMembers(@PathVariable UUID id, @RequestBody UserIds in) {
        catalog.removeMembers(id, in.userIds());
        return ApiResponse.ok(null, "成员移除成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_ADMIN_ADD)
    @PostMapping("/api/groups/{id}/admins")
    ApiResponse<Void> addAdmins(@PathVariable UUID id, @RequestBody UserIds in) {
        catalog.addAdmins(id, in.userIds());
        return ApiResponse.ok(null, "管理员添加成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_ADMIN_REMOVE)
    @DeleteMapping("/api/groups/{id}/admins")
    ApiResponse<Void> removeAdmins(@PathVariable UUID id, @RequestBody UserIds in) {
        catalog.removeAdmins(id, in.userIds());
        return ApiResponse.ok(null, "管理员移除成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_LIST)
    @GetMapping("/api/positions")
    ApiResponse<?> positions(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                             @RequestParam(required = false) String name, @RequestParam(required = false) String code,
                             @RequestParam(required = false) UUID departmentId, @RequestParam(required = false) Integer level) {
        var x = catalog.positions(page, pageSize, name, code, departmentId, level);
        return ApiResponse.ok(PageData.of(x.data(), x.page(), x.pageSize(), x.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_STATS)
    @GetMapping("/api/positions/stats")
    ApiResponse<?> positionStats() {
        return ApiResponse.ok(catalog.positionStats());
    }

    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_READ)
    @GetMapping("/api/positions/{id}")
    ApiResponse<?> position(@PathVariable UUID id) {
        return ApiResponse.ok(catalog.position(id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_CREATE)
    @PostMapping("/api/positions")
    ApiResponse<?> createPosition(@RequestBody DirectoryCatalogService.PositionInput in) {
        return ApiResponse.ok(catalog.createPosition(in), "岗位创建成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_UPDATE)
    @PutMapping("/api/positions/{id}")
    ApiResponse<?> updatePosition(@PathVariable UUID id, @RequestBody DirectoryCatalogService.PositionInput in) {
        return ApiResponse.ok(catalog.updatePosition(id, in), "岗位更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_DELETE)
    @DeleteMapping("/api/positions/{id}")
    ApiResponse<Void> deletePosition(@PathVariable UUID id) {
        catalog.deletePosition(id);
        return ApiResponse.ok(null, "岗位删除成功");
    }

    public record UserIds(List<UUID> userIds) {
        public UserIds {
            userIds = userIds == null ? List.of() : List.copyOf(userIds);
        }
    }
}
