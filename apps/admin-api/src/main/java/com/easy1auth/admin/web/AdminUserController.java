package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantContextFilter;
import com.easy1auth.adminaccess.AdminAccessService;
import com.easy1auth.adminidentity.AdminIdentityService;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin-users")
public class AdminUserController {
    private final AdminAccessService access; private final AdminIdentityService identities; private final com.easy1auth.tenant.TenantService tenants;
    AdminUserController(AdminAccessService access,AdminIdentityService identities,com.easy1auth.tenant.TenantService tenants){this.access=access;this.identities=identities;this.tenants=tenants;}
    @GetMapping public ApiResponse<?> list(HttpServletRequest r,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int pageSize,@RequestParam(required=false)String username,@RequestParam(required=false)String email,@RequestParam(required=false)String status,@RequestParam(required=false)UUID roleId){return ApiResponse.ok(access.members(context(r),page,pageSize,username,email,status,roleId));}
    @GetMapping("/stats") public ApiResponse<?> stats(HttpServletRequest r){return ApiResponse.ok(access.memberStats(context(r)));}
    @GetMapping("/{id}") public ApiResponse<?> get(HttpServletRequest r,@PathVariable UUID id){return ApiResponse.ok(access.member(context(r),id));}
    @PutMapping("/{id}") public ApiResponse<?> update(HttpServletRequest r,@PathVariable UUID id,@RequestBody ProfileInput in){var c=context(r);access.require(c,"admin-user:update");access.member(c,id);identities.updateProfile(id,in.username(),in.email(),in.phone());return ApiResponse.ok(access.member(c,id),"管理员信息更新成功");}
    @PutMapping("/{id}/status") public ApiResponse<?> status(HttpServletRequest r,@PathVariable UUID id,@RequestBody StatusInput in){var c=context(r);access.require(c,"admin-user:status");access.member(c,id);if("disabled".equals(in.status())&&tenants.isOwnerAnywhere(id))throw new com.easy1auth.foundation.error.DomainException("OWNER_STATUS_IMMUTABLE","账号仍是某个租户的 owner，必须先转移所有权",409);identities.updateStatus(c.accountId(),id,in.status());return ApiResponse.ok(access.member(c,id),"管理员状态更新成功");}
    @PostMapping("/{id}/reset-password") public ApiResponse<?> password(HttpServletRequest r,@PathVariable UUID id,@RequestBody PasswordInput in){var c=context(r);access.require(c,"admin-user:reset-password");access.member(c,id);identities.resetPassword(id,in.newPassword());return ApiResponse.ok(access.member(c,id),"管理员密码重置成功");}
    @PostMapping("/{id}/reset-mfa") public ApiResponse<?> mfa(HttpServletRequest r,@PathVariable UUID id){var c=context(r);access.require(c,"admin-user:reset-mfa");access.member(c,id);identities.resetMfa(id);return ApiResponse.ok(access.member(c,id),"管理员 MFA 重置成功");}
    @PutMapping("/{id}/roles") public ApiResponse<?> roles(HttpServletRequest r,@PathVariable UUID id,@RequestBody RolesInput in){var c=context(r);access.assignRoles(c,id,in.roleIds());return ApiResponse.ok(access.member(c,id),"管理员角色分配成功");}
    @DeleteMapping("/{id}/tenant") public ApiResponse<Void> remove(HttpServletRequest r,@PathVariable UUID id){access.removeMember(context(r),id);return ApiResponse.ok(null,"管理员已移出租户");}
    private static TenantContext context(HttpServletRequest r){return (TenantContext)Objects.requireNonNull(r.getAttribute(TenantContextFilter.ATTRIBUTE));}
    public record ProfileInput(String username,String email,String phone){} public record StatusInput(String status){} public record PasswordInput(String newPassword){} public record RolesInput(List<UUID> roleIds){}
}
