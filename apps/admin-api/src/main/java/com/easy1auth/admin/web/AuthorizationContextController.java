package com.easy1auth.admin.web;

import com.easy1auth.admin.web.dto.AuthorizationContextResponse;
import com.easy1auth.framework.tenant.context.TenantContextHolder;
import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.admin.annotation.ManagementRouteClassification;
import com.easy1auth.admin.constant.ManagementRouteKind;
import com.easy1auth.system.ManagementPermissionCatalog;
import com.easy1auth.system.constant.ManagementPermissionType;
import com.easy1auth.system.dto.ManagementPermissionView;
import com.easy1auth.framework.web.response.ApiResponse;
import com.easy1auth.tenant.dto.TenantAccess;
import com.easy1auth.tenant.service.TenantService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 授权上下文接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/authorization}，返回当前租户上下文下的
 * 成员角色、权限集合、租户套餐以及可见菜单，供前端初始化界面与菜单渲染使用。
 * 该接口以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/authorization")
public class AuthorizationContextController {
    /** 管理权限目录 */
    private final ManagementPermissionCatalog catalog;
    /** 租户服务 */
    private final TenantService tenants;

    AuthorizationContextController(ManagementPermissionCatalog catalog, TenantService tenants) {
        this.catalog = catalog;
        this.tenants = tenants;
    }

    /** 查询当前租户授权上下文（角色、权限、套餐与可见菜单）。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHORIZATION_CONTEXT)
    @GetMapping("/context")
    public ApiResponse<AuthorizationContextResponse> context(@AuthenticationPrincipal Jwt principal) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new DomainException(com.easy1auth.tenant.constant.ErrorCodeConstants.TENANT_CONTEXT_REQUIRED);
        }
        TenantAccess access = tenants.authorize(UUID.fromString(principal.getSubject()), tenantId);
        return ApiResponse.ok(new AuthorizationContextResponse(
                access.tenantId(),
                access.membershipRole(),
                access.permissions().stream().sorted().toList(),
                access.tenantPackage(),
                visibleMenus(access)));
    }

    /** 从权限目录中筛选当前上下文可见的菜单与目录。 */
    private List<ManagementPermissionView> visibleMenus(TenantAccess access) {
        return catalog.activeViews().stream()
                .filter(permission -> permission.type() == ManagementPermissionType.MENU
                        || permission.type() == ManagementPermissionType.DIRECTORY)
                .filter(permission -> access.permissions().contains(permission.code()))
                .toList();
    }

    /**
     * 授权上下文响应。
     *
     * @param tenantId        当前租户 ID
     * @param membershipRole  账号在租户内的成员角色
     * @param permissions     当前账号在租户下拥有的权限代码列表（已排序）
     * @param tenantPackage   租户绑定的套餐视图
     * @param menus           当前上下文可见的菜单与目录
     */
}
