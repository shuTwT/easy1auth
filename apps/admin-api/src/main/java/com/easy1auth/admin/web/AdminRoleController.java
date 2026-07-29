package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantContextFilter;
import com.easy1auth.adminaccess.*;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin-roles")
public class AdminRoleController {
    private final AdminAccessService access;
    AdminRoleController(AdminAccessService access){this.access=access;}
    @GetMapping("/stats") public ApiResponse<?> stats(HttpServletRequest r){return ApiResponse.ok(access.roleStats(context(r)));}
    @GetMapping("/permissions/catalog") public ApiResponse<?> catalog(HttpServletRequest r){access.require(context(r),"admin-role:read");return ApiResponse.ok(Map.of("permissions",AdminAccessService.PERMISSIONS));}
    @GetMapping public ApiResponse<?> list(HttpServletRequest r,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int pageSize,@RequestParam(required=false) String name,@RequestParam(required=false) Boolean isSystem){return ApiResponse.ok(access.roles(context(r),page,pageSize,name,isSystem));}
    @GetMapping("/{id}") public ApiResponse<?> get(HttpServletRequest r,@PathVariable UUID id){return ApiResponse.ok(access.role(context(r),id));}
    @PostMapping public ApiResponse<?> create(HttpServletRequest r,@RequestBody RoleInput in){return ApiResponse.ok(access.create(context(r),in.name(),in.description(),in.permissions()),"管理员角色创建成功");}
    @PutMapping("/{id}") public ApiResponse<?> update(HttpServletRequest r,@PathVariable UUID id,@RequestBody RoleInput in){return ApiResponse.ok(access.update(context(r),id,in.name(),in.description(),in.permissions()),"管理员角色更新成功");}
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(HttpServletRequest r,@PathVariable UUID id){access.delete(context(r),id);return ApiResponse.ok(null,"管理员角色删除成功");}
    private static TenantContext context(HttpServletRequest r){return (TenantContext)Objects.requireNonNull(r.getAttribute(TenantContextFilter.ATTRIBUTE));}
    public record RoleInput(String name,String description,List<String> permissions){}
}
