package com.easy1auth.admin.web;

import com.easy1auth.adminaccess.AdminAccessService;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.admin.security.PlatformManagementPermission;
import com.easy1auth.adminidentity.AdminIdentityService;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.tenant.TenantDataBoundary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin-users")
public class AdminUserController {
    private final AdminAccessService access;
    private final AdminIdentityService identities;
    private final PlatformAuthorizationResolver platformAuthorization;

    AdminUserController(AdminAccessService access, AdminIdentityService identities,
                        PlatformAuthorizationResolver platformAuthorization) {
        this.access = access;
        this.identities = identities;
        this.platformAuthorization = platformAuthorization;
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_LIST, boundary = TenantDataBoundary.PLATFORM_ALL)
    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal Jwt actor, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String username, @RequestParam(required = false) String email, @RequestParam(required = false) String status, @RequestParam(required = false) UUID roleId) {
        require(actor, ManagementPermissionCode.ADMIN_USER_LIST);
        var p = access.members(page, pageSize, username, email, status, roleId);
        return ApiResponse.ok(PageData.of(p.admins(), p.page(), p.pageSize(), p.total()));
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_STATS, boundary = TenantDataBoundary.PLATFORM_ALL)
    @GetMapping("/stats")
    public ApiResponse<?> stats(@AuthenticationPrincipal Jwt actor) {
        require(actor, ManagementPermissionCode.ADMIN_USER_STATS);
        return ApiResponse.ok(access.memberStats());
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_READ, boundary = TenantDataBoundary.PLATFORM_ALL)
    @GetMapping("/{id}")
    public ApiResponse<?> get(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id) {
        require(actor, ManagementPermissionCode.ADMIN_USER_READ);
        return ApiResponse.ok(access.member(id));
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_CREATE, boundary = TenantDataBoundary.PLATFORM_ALL)
    @PostMapping
    public ApiResponse<?> create(@AuthenticationPrincipal Jwt actor, @RequestBody CreateInput in) {
        require(actor, ManagementPermissionCode.ADMIN_USER_CREATE);
        return ApiResponse.ok(identities.createAdministrator(in.username(), in.email(), in.password()), "管理员账号创建成功，尚未分配租户");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_UPDATE, boundary = TenantDataBoundary.PLATFORM_ALL)
    @PutMapping("/{id}")
    public ApiResponse<?> update(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id, @RequestBody ProfileInput in) {
        require(actor, ManagementPermissionCode.ADMIN_USER_UPDATE);
        access.member(id);
        identities.updateProfile(id, in.username(), in.email(), in.phone());
        return ApiResponse.ok(access.member(id), "管理员信息更新成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_STATUS, boundary = TenantDataBoundary.PLATFORM_ALL)
    @PutMapping("/{id}/status")
    public ApiResponse<?> status(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id, @RequestBody StatusInput in) {
        require(actor, ManagementPermissionCode.ADMIN_USER_STATUS);
        access.member(id);
        access.updateMemberStatus(accountId(actor), id, in.status());
        return ApiResponse.ok(access.member(id), "管理员状态更新成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_RESET_PASSWORD, boundary = TenantDataBoundary.PLATFORM_ALL)
    @PostMapping("/{id}/reset-password")
    public ApiResponse<?> password(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id, @RequestBody PasswordInput in) {
        require(actor, ManagementPermissionCode.ADMIN_USER_RESET_PASSWORD);
        access.member(id);
        identities.resetPassword(accountId(actor), id, in.newPassword());
        return ApiResponse.ok(access.member(id), "管理员密码重置成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_RESET_MFA, boundary = TenantDataBoundary.PLATFORM_ALL)
    @PostMapping("/{id}/reset-mfa")
    public ApiResponse<?> mfa(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id) {
        require(actor, ManagementPermissionCode.ADMIN_USER_RESET_MFA);
        access.member(id);
        identities.resetMfa(accountId(actor), id);
        return ApiResponse.ok(access.member(id), "管理员 MFA 重置成功");
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_ASSIGN_ROLE, boundary = TenantDataBoundary.PLATFORM_ALL)
    @PutMapping("/{id}/roles")
    public ApiResponse<?> roles(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id, @RequestBody RolesInput in) {
        require(actor, ManagementPermissionCode.ADMIN_USER_ASSIGN_ROLE);
        return ApiResponse.ok(access.assignRoles(in.tenantId(), id, in.roleIds()), "管理员角色已作为未来元数据保存，当前不影响授权或租户管理员关系");
    }

    private void require(Jwt actor, ManagementPermissionCode permission) {
        platformAuthorization.require(accountId(actor), permission);
    }

    private static UUID accountId(Jwt actor) {
        if (actor == null || actor.getSubject() == null) {
            throw new DomainException("AUTHENTICATION_SUBJECT_INVALID", "认证主体无效", 403);
        }
        try {
            return UUID.fromString(actor.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new DomainException("AUTHENTICATION_SUBJECT_INVALID", "认证主体无效", 403);
        }
    }

    public record CreateInput(String username, String email, String password) {
    }

    public record ProfileInput(String username, String email, String phone) {
    }

    public record StatusInput(String status) {
    }

    public record PasswordInput(String newPassword) {
    }

    public record RolesInput(UUID tenantId, List<UUID> roleIds) {
    }
}
