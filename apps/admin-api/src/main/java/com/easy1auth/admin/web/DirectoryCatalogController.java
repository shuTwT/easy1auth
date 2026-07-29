package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantContextFilter;
import com.easy1auth.directory.DirectoryCatalogService;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class DirectoryCatalogController {
    private final DirectoryCatalogService catalog;
    DirectoryCatalogController(DirectoryCatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/api/groups")
    ApiResponse<?> groups(HttpServletRequest r, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize,
                          @RequestParam(required=false) String name, @RequestParam(required=false) String type, @RequestParam(required=false) UUID parentId) {
        var x = catalog.groups(c(r).tenantId(), page, pageSize, name, type, parentId);
        return ApiResponse.ok(Map.of("groups", x.data(), "total", x.total(), "page", x.page(), "pageSize", x.pageSize()));
    }
    @GetMapping("/api/groups/tree") ApiResponse<?> groupTree(HttpServletRequest r) { return ApiResponse.ok(catalog.groupTree(c(r).tenantId())); }
    @GetMapping("/api/groups/stats") ApiResponse<?> groupStats(HttpServletRequest r) { return ApiResponse.ok(catalog.groupStats(c(r).tenantId())); }
    @GetMapping("/api/groups/{id}") ApiResponse<?> group(HttpServletRequest r, @PathVariable UUID id) { return ApiResponse.ok(catalog.group(c(r).tenantId(), id)); }
    @PostMapping("/api/groups") ApiResponse<?> createGroup(HttpServletRequest r, @RequestBody DirectoryCatalogService.GroupInput in) { return ApiResponse.ok(catalog.createGroup(c(r).tenantId(), in), "用户组创建成功"); }
    @PutMapping("/api/groups/{id}") ApiResponse<?> updateGroup(HttpServletRequest r, @PathVariable UUID id, @RequestBody DirectoryCatalogService.GroupInput in) { return ApiResponse.ok(catalog.updateGroup(c(r).tenantId(), id, in), "用户组更新成功"); }
    @DeleteMapping("/api/groups/{id}") ApiResponse<Void> deleteGroup(HttpServletRequest r, @PathVariable UUID id) { catalog.deleteGroup(c(r).tenantId(), id); return ApiResponse.ok(null, "用户组删除成功"); }
    @GetMapping("/api/groups/{id}/members") ApiResponse<?> groupMembers(HttpServletRequest r, @PathVariable UUID id) { return ApiResponse.ok(catalog.groupMembers(c(r).tenantId(), id)); }
    @PostMapping("/api/groups/{id}/members") ApiResponse<Void> addMembers(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) { catalog.addMembers(c(r).tenantId(), id, in.userIds()); return ApiResponse.ok(null, "成员添加成功"); }
    @DeleteMapping("/api/groups/{id}/members") ApiResponse<Void> removeMembers(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) { catalog.removeMembers(c(r).tenantId(), id, in.userIds()); return ApiResponse.ok(null, "成员移除成功"); }
    @PostMapping("/api/groups/{id}/admins") ApiResponse<Void> addAdmins(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) { catalog.addAdmins(c(r).tenantId(), id, in.userIds()); return ApiResponse.ok(null, "管理员添加成功"); }
    @DeleteMapping("/api/groups/{id}/admins") ApiResponse<Void> removeAdmins(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) { catalog.removeAdmins(c(r).tenantId(), id, in.userIds()); return ApiResponse.ok(null, "管理员移除成功"); }

    @GetMapping("/api/positions")
    ApiResponse<?> positions(HttpServletRequest r, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize,
                             @RequestParam(required=false) String name, @RequestParam(required=false) String code,
                             @RequestParam(required=false) UUID departmentId, @RequestParam(required=false) Integer level) {
        var x = catalog.positions(c(r).tenantId(), page, pageSize, name, code, departmentId, level);
        return ApiResponse.ok(Map.of("positions", x.data(), "total", x.total(), "page", x.page(), "pageSize", x.pageSize()));
    }
    @GetMapping("/api/positions/stats") ApiResponse<?> positionStats(HttpServletRequest r) { return ApiResponse.ok(catalog.positionStats(c(r).tenantId())); }
    @GetMapping("/api/positions/{id}") ApiResponse<?> position(HttpServletRequest r, @PathVariable UUID id) { return ApiResponse.ok(catalog.position(c(r).tenantId(), id)); }
    @PostMapping("/api/positions") ApiResponse<?> createPosition(HttpServletRequest r, @RequestBody DirectoryCatalogService.PositionInput in) { return ApiResponse.ok(catalog.createPosition(c(r).tenantId(), in), "岗位创建成功"); }
    @PutMapping("/api/positions/{id}") ApiResponse<?> updatePosition(HttpServletRequest r, @PathVariable UUID id, @RequestBody DirectoryCatalogService.PositionInput in) { return ApiResponse.ok(catalog.updatePosition(c(r).tenantId(), id, in), "岗位更新成功"); }
    @DeleteMapping("/api/positions/{id}") ApiResponse<Void> deletePosition(HttpServletRequest r, @PathVariable UUID id) { catalog.deletePosition(c(r).tenantId(), id); return ApiResponse.ok(null, "岗位删除成功"); }

    private static TenantContext c(HttpServletRequest r) { return (TenantContext) Objects.requireNonNull(r.getAttribute(TenantContextFilter.ATTRIBUTE)); }
    public record UserIds(List<UUID> userIds) { public UserIds { userIds = userIds == null ? List.of() : List.copyOf(userIds); } }
}
