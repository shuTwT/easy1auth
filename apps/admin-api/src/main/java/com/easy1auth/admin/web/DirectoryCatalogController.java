package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.poolidentity.service.DirectoryCatalogService;
import com.easy1auth.poolidentity.service.GroupInput;
import com.easy1auth.poolidentity.service.PositionInput;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 组织架构目录接口（用户组与岗位）。
 *
 * <p>管理端 REST 入口，提供用户组（{@code /api/groups}）与岗位（{@code /api/positions}）
 * 的分页查询、树形结构、统计、增删改以及成员 / 管理员管理能力。所有操作均通过
 * {@link TenantManagementPermission} 做租户级权限控制，并以 {@link ApiResponse}
 * 统一包装返回。</p>
 */
@RestController
public class DirectoryCatalogController {
    /** 组织架构目录服务 */
    private final DirectoryCatalogService catalog;

    DirectoryCatalogController(DirectoryCatalogService catalog) {
        this.catalog = catalog;
    }

    /** 分页查询用户组列表，支持按名称、类型与上级组过滤。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_LIST)
    @GetMapping("/api/groups")
    ApiResponse<?> groups(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String name, @RequestParam(required = false) String type, @RequestParam(required = false) UUID parentId) {
        var x = catalog.groups(page, pageSize, name, type, parentId);
        return ApiResponse.ok(x);
    }

    /** 查询用户组的树形结构。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_TREE)
    @GetMapping("/api/groups/tree")
    ApiResponse<?> groupTree() {
        return ApiResponse.ok(catalog.groupTree());
    }

    /** 查询用户组的统计信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_STATS)
    @GetMapping("/api/groups/stats")
    ApiResponse<?> groupStats() {
        return ApiResponse.ok(catalog.groupStats());
    }

    /** 查询指定用户组的详情。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_READ)
    @GetMapping("/api/groups/{id}")
    ApiResponse<?> group(@PathVariable UUID id) {
        return ApiResponse.ok(catalog.group(id));
    }

    /** 创建用户组。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_CREATE)
    @PostMapping("/api/groups")
    ApiResponse<?> createGroup(@RequestBody GroupInput in) {
        return ApiResponse.ok(catalog.createGroup(in), "用户组创建成功");
    }

    /** 更新指定用户组的信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_UPDATE)
    @PutMapping("/api/groups/{id}")
    ApiResponse<?> updateGroup(@PathVariable UUID id, @RequestBody GroupInput in) {
        return ApiResponse.ok(catalog.updateGroup(id, in), "用户组更新成功");
    }

    /** 删除指定用户组。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_DELETE)
    @DeleteMapping("/api/groups/{id}")
    ApiResponse<Void> deleteGroup(@PathVariable UUID id) {
        catalog.deleteGroup(id);
        return ApiResponse.ok(null, "用户组删除成功");
    }

    /** 查询指定用户组的成员列表。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_MEMBERS_READ)
    @GetMapping("/api/groups/{id}/members")
    ApiResponse<?> groupMembers(@PathVariable UUID id) {
        return ApiResponse.ok(catalog.groupMembers(id));
    }

    /** 向指定用户组批量添加成员。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_MEMBERS_ADD)
    @PostMapping("/api/groups/{id}/members")
    ApiResponse<Void> addMembers(@PathVariable UUID id, @RequestBody UserIds in) {
        catalog.addMembers(id, in.userIds());
        return ApiResponse.ok(null, "成员添加成功");
    }

    /** 从指定用户组批量移除成员。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_MEMBERS_REMOVE)
    @DeleteMapping("/api/groups/{id}/members")
    ApiResponse<Void> removeMembers(@PathVariable UUID id, @RequestBody UserIds in) {
        catalog.removeMembers(id, in.userIds());
        return ApiResponse.ok(null, "成员移除成功");
    }

    /** 向指定用户组批量添加管理员。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_ADMIN_ADD)
    @PostMapping("/api/groups/{id}/admins")
    ApiResponse<Void> addAdmins(@PathVariable UUID id, @RequestBody UserIds in) {
        catalog.addAdmins(id, in.userIds());
        return ApiResponse.ok(null, "管理员添加成功");
    }

    /** 从指定用户组批量移除管理员。 */
    @TenantManagementPermission(value = ManagementPermissionCode.GROUP_ADMIN_REMOVE)
    @DeleteMapping("/api/groups/{id}/admins")
    ApiResponse<Void> removeAdmins(@PathVariable UUID id, @RequestBody UserIds in) {
        catalog.removeAdmins(id, in.userIds());
        return ApiResponse.ok(null, "管理员移除成功");
    }

    /** 分页查询岗位列表，支持按名称、编码、部门与层级过滤。 */
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_LIST)
    @GetMapping("/api/positions")
    ApiResponse<?> positions(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                             @RequestParam(required = false) String name, @RequestParam(required = false) String code,
                             @RequestParam(required = false) UUID departmentId, @RequestParam(required = false) Integer level) {
        var x = catalog.positions(page, pageSize, name, code, departmentId, level);
        return ApiResponse.ok(x);
    }

    /** 查询岗位的统计信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_STATS)
    @GetMapping("/api/positions/stats")
    ApiResponse<?> positionStats() {
        return ApiResponse.ok(catalog.positionStats());
    }

    /** 查询指定岗位的详情。 */
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_READ)
    @GetMapping("/api/positions/{id}")
    ApiResponse<?> position(@PathVariable UUID id) {
        return ApiResponse.ok(catalog.position(id));
    }

    /** 创建岗位。 */
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_CREATE)
    @PostMapping("/api/positions")
    ApiResponse<?> createPosition(@RequestBody PositionInput in) {
        return ApiResponse.ok(catalog.createPosition(in), "岗位创建成功");
    }

    /** 更新指定岗位的信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_UPDATE)
    @PutMapping("/api/positions/{id}")
    ApiResponse<?> updatePosition(@PathVariable UUID id, @RequestBody PositionInput in) {
        return ApiResponse.ok(catalog.updatePosition(id, in), "岗位更新成功");
    }

    /** 删除指定岗位。 */
    @TenantManagementPermission(value = ManagementPermissionCode.POSITION_DELETE)
    @DeleteMapping("/api/positions/{id}")
    ApiResponse<Void> deletePosition(@PathVariable UUID id) {
        catalog.deletePosition(id);
        return ApiResponse.ok(null, "岗位删除成功");
    }

    /**
     * 用户 ID 集合请求体（用于批量添加 / 移除成员或管理员）。
     *
     * @param userIds 用户 ID 列表，为空时按空列表处理
     */
    public record UserIds(List<UUID> userIds) {
        public UserIds {
            userIds = userIds == null ? List.of() : List.copyOf(userIds);
        }
    }
}
