package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantContextFilter;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.TenantContext;
import com.easy1auth.useraccess.UserAccessCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class UserAccessCatalogController {
    private final UserAccessCatalogService service;
    UserAccessCatalogController(UserAccessCatalogService service) { this.service = service; }

    @GetMapping("/api/roles") Object roles(HttpServletRequest r, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int pageSize, @RequestParam(required=false) String search, @RequestParam(required=false) String type) { return service.roles(c(r).tenantId(), page, pageSize, search, type); }
    @GetMapping("/api/roles/stats") Object roleStats(HttpServletRequest r) { return service.roleStats(c(r).tenantId()); }
    @GetMapping("/api/roles/tree") Object roleTree(HttpServletRequest r) { return service.roleTree(c(r).tenantId()); }
    @GetMapping("/api/roles/{id}") Object role(HttpServletRequest r, @PathVariable UUID id) { return service.role(c(r).tenantId(), id); }
    @PostMapping("/api/roles") ResponseEntity<?> createRole(HttpServletRequest r, @RequestBody UserAccessCatalogService.RoleInput in) { return ResponseEntity.status(201).body(service.createRole(c(r).tenantId(), in)); }
    @PutMapping("/api/roles/{id}") Object updateRole(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserAccessCatalogService.RoleInput in) { return service.updateRole(c(r).tenantId(), id, in); }
    @DeleteMapping("/api/roles/{id}") ResponseEntity<Void> deleteRole(HttpServletRequest r, @PathVariable UUID id) { service.deleteRole(c(r).tenantId(), id); return ResponseEntity.noContent().build(); }
    @GetMapping("/api/roles/{id}/users") Object roleUsers(HttpServletRequest r, @PathVariable UUID id, @RequestParam(required=false) String search) { return service.roleUsers(c(r).tenantId(), id, search); }
    @PostMapping("/api/roles/{id}/users") Map<String,String> assignUsers(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) { service.assignUsers(c(r).tenantId(), id, in.userIds()); return Map.of("message", "分配用户成功"); }
    @DeleteMapping("/api/roles/{id}/users") Map<String,String> removeUsers(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserIds in) { service.removeUsers(c(r).tenantId(), id, in.userIds()); return Map.of("message", "移除用户成功"); }
    @GetMapping("/api/roles/user/{userId}") Object userRoles(HttpServletRequest r, @PathVariable UUID userId) { return service.rolesForUser(c(r).tenantId(), userId); }
    @PostMapping("/api/roles/user/{userId}") Map<String,String> replaceRoles(HttpServletRequest r, @PathVariable UUID userId, @RequestBody RoleIds in) { service.replaceUserRoles(c(r).tenantId(), userId, in.roleIds()); return Map.of("message", "分配角色成功"); }

    @GetMapping("/api/permissions") ApiResponse<?> permissions(HttpServletRequest r, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="50") int pageSize, @RequestParam(required=false) String search, @RequestParam(required=false) String type, @RequestParam(required=false) String resource) { return ApiResponse.ok(service.permissions(c(r).tenantId(), page, pageSize, search, type, resource)); }
    @GetMapping("/api/permissions/stats") ApiResponse<?> permissionStats(HttpServletRequest r) { return ApiResponse.ok(service.permissionStats(c(r).tenantId())); }
    @GetMapping("/api/permissions/tree") ApiResponse<?> permissionTree(HttpServletRequest r) { return ApiResponse.ok(service.permissionTree(c(r).tenantId())); }
    @GetMapping("/api/permissions/{id}") ApiResponse<?> permission(HttpServletRequest r, @PathVariable UUID id) { return ApiResponse.ok(service.permission(c(r).tenantId(), id)); }
    @PostMapping("/api/permissions") ApiResponse<?> createPermission(HttpServletRequest r, @RequestBody UserAccessCatalogService.PermissionInput in) { return ApiResponse.ok(service.createPermission(c(r).tenantId(), in), "创建权限成功"); }
    @PutMapping("/api/permissions/{id}") ApiResponse<?> updatePermission(HttpServletRequest r, @PathVariable UUID id, @RequestBody UserAccessCatalogService.PermissionInput in) { return ApiResponse.ok(service.updatePermission(c(r).tenantId(), id, in), "更新权限成功"); }
    @DeleteMapping("/api/permissions/{id}") ApiResponse<Void> deletePermission(HttpServletRequest r, @PathVariable UUID id) { service.deletePermission(c(r).tenantId(), id); return ApiResponse.ok(null, "删除权限成功"); }

    private static TenantContext c(HttpServletRequest r) { return (TenantContext) Objects.requireNonNull(r.getAttribute(TenantContextFilter.ATTRIBUTE)); }
    public record UserIds(List<UUID> userIds) { public UserIds { userIds = userIds == null ? List.of() : List.copyOf(userIds); } }
    public record RoleIds(List<UUID> roleIds) { public RoleIds { roleIds = roleIds == null ? List.of() : List.copyOf(roleIds); } }
}
