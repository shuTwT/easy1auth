package com.easy1auth.admin.web;

import com.easy1auth.adminaccess.AdminAccessService;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.admin.security.PlatformManagementPermission;
import com.easy1auth.adminidentity.AdminIdentityService;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.web.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 平台管理员账号管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/admin-users}，面向平台管理侧提供管理员
 * （admin_user）账号的分页查询、统计、详情、创建、更新、启停、密码重置、MFA 重置
 * 与角色分配能力。所有操作均通过 {@link PlatformManagementPermission} 做平台级
 * 权限控制，并以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/admin-users")
public class AdminUserController {
    /** 管理访问服务（管理员成员与角色） */
    private final AdminAccessService access;
    /** 管理账号身份服务 */
    private final AdminIdentityService identities;
    /** 平台级授权解析器 */
    private final PlatformAuthorizationResolver platformAuthorization;

    AdminUserController(AdminAccessService access, AdminIdentityService identities,
                        PlatformAuthorizationResolver platformAuthorization) {
        this.access = access;
        this.identities = identities;
        this.platformAuthorization = platformAuthorization;
    }

    /** 分页查询管理员账号列表，支持按用户名、邮箱、状态与角色过滤。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_LIST)
    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal Jwt actor, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String username, @RequestParam(required = false) String email, @RequestParam(required = false) String status, @RequestParam(required = false) UUID roleId) {
        require(actor, ManagementPermissionCode.ADMIN_USER_LIST);
        var p = access.members(page, pageSize, username, email, status, roleId);
        return ApiResponse.ok(p);
    }

    /** 查询管理员账号的统计信息。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_STATS)
    @GetMapping("/stats")
    public ApiResponse<?> stats(@AuthenticationPrincipal Jwt actor) {
        require(actor, ManagementPermissionCode.ADMIN_USER_STATS);
        return ApiResponse.ok(access.memberStats());
    }

    /** 查询指定管理员账号的详情。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_READ)
    @GetMapping("/{id}")
    public ApiResponse<?> get(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id) {
        require(actor, ManagementPermissionCode.ADMIN_USER_READ);
        return ApiResponse.ok(access.member(id));
    }

    /** 创建管理员账号（创建后尚未分配租户）。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_CREATE)
    @PostMapping
    public ApiResponse<?> create(@AuthenticationPrincipal Jwt actor, @RequestBody CreateInput in) {
        require(actor, ManagementPermissionCode.ADMIN_USER_CREATE);
        return ApiResponse.ok(identities.createAdministrator(in.username(), in.email(), in.password()), "管理员账号创建成功，尚未分配租户");
    }

    /** 更新指定管理员账号的资料（用户名、邮箱、手机号）。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<?> update(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id, @RequestBody ProfileInput in) {
        require(actor, ManagementPermissionCode.ADMIN_USER_UPDATE);
        access.member(id);
        identities.updateProfile(id, in.username(), in.email(), in.phone());
        return ApiResponse.ok(access.member(id), "管理员信息更新成功");
    }

    /** 更新指定管理员账号的启停状态。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_STATUS)
    @PutMapping("/{id}/status")
    public ApiResponse<?> status(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id, @RequestBody StatusInput in) {
        require(actor, ManagementPermissionCode.ADMIN_USER_STATUS);
        access.member(id);
        access.updateMemberStatus(accountId(actor), id, in.status());
        return ApiResponse.ok(access.member(id), "管理员状态更新成功");
    }

    /** 重置指定管理员账号的登录密码。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_RESET_PASSWORD)
    @PostMapping("/{id}/reset-password")
    public ApiResponse<?> password(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id, @RequestBody PasswordInput in) {
        require(actor, ManagementPermissionCode.ADMIN_USER_RESET_PASSWORD);
        access.member(id);
        identities.resetPassword(accountId(actor), id, in.newPassword());
        return ApiResponse.ok(access.member(id), "管理员密码重置成功");
    }

    /** 重置指定管理员账号的 MFA 绑定。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_RESET_MFA)
    @PostMapping("/{id}/reset-mfa")
    public ApiResponse<?> mfa(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id) {
        require(actor, ManagementPermissionCode.ADMIN_USER_RESET_MFA);
        access.member(id);
        identities.resetMfa(accountId(actor), id);
        return ApiResponse.ok(access.member(id), "管理员 MFA 重置成功");
    }

    /** 为指定管理员账号在指定租户内分配角色（当前仅保存为元数据，不影响实时授权）。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.ADMIN_USER_ASSIGN_ROLE)
    @PutMapping("/{id}/roles")
    public ApiResponse<?> roles(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id, @RequestBody RolesInput in) {
        require(actor, ManagementPermissionCode.ADMIN_USER_ASSIGN_ROLE);
        return ApiResponse.ok(access.assignRoles(in.tenantId(), id, in.roleIds()), "管理员角色已作为未来元数据保存，当前不影响授权或租户管理员关系");
    }

    /** 校验当前账号是否具备指定平台权限，不具备时抛出授权异常。 */
    private void require(Jwt actor, ManagementPermissionCode permission) {
        platformAuthorization.require(accountId(actor), permission);
    }

    /** 从 JWT 主体解析当前账号 ID，缺失或非法时抛出认证异常。 */
    private static UUID accountId(Jwt actor) {
        if (actor == null || actor.getSubject() == null) {
            throw new DomainException(ErrorCodeConstants.AUTHENTICATION_SUBJECT_INVALID);
        }
        try {
            return UUID.fromString(actor.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new DomainException(ErrorCodeConstants.AUTHENTICATION_SUBJECT_INVALID);
        }
    }

    /**
     * 管理员创建输入。
     *
     * @param username 用户名
     * @param email    邮箱
     * @param password 初始密码
     */
    public record CreateInput(String username, String email, String password) {
    }

    /**
     * 管理员资料更新输入。
     *
     * @param username 用户名（可选）
     * @param email    邮箱（可选）
     * @param phone    手机号（可选）
     */
    public record ProfileInput(String username, String email, String phone) {
    }

    /**
     * 管理员状态输入。
     *
     * @param status 目标状态：active / disabled
     */
    public record StatusInput(String status) {
    }

    /**
     * 密码重置输入。
     *
     * @param newPassword 新密码
     */
    public record PasswordInput(String newPassword) {
    }

    /**
     * 角色分配输入。
     *
     * @param tenantId 目标租户 ID
     * @param roleIds  待分配的角色 ID 列表
     */
    public record RolesInput(UUID tenantId, List<UUID> roleIds) {
    }
}
