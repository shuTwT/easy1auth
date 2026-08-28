package com.easy1auth.admin.web;

import com.easy1auth.tenant.util.WebFramework;

import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
import com.easy1auth.adminaccess.ManagementPermissionCatalog;
import com.easy1auth.adminaccess.ManagementPermissionType;
import com.easy1auth.adminaccess.ManagementPermissionView;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import com.easy1auth.tenant.util.TenantContext;
import com.easy1auth.tenant.dto.TenantPackageView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 授权上下文接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/authorization}，返回当前租户上下文下的
 * 成员角色、权限集合、租户套餐以及可见菜单，供前端初始化界面与菜单渲染使用。
 * 该接口基于 {@link TenantContext} 的已解析授权信息，并以 {@link ApiResponse}
 * 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/authorization")
public class AuthorizationContextController {
    /** 管理权限目录 */
    private final ManagementPermissionCatalog catalog;

    AuthorizationContextController(ManagementPermissionCatalog catalog) {
        this.catalog = catalog;
    }

    /** 查询当前租户授权上下文（角色、权限、套餐与可见菜单）。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHORIZATION_CONTEXT)
    @GetMapping("/context")
    public ApiResponse<AuthorizationContextResponse> context(
            @RequestAttribute(WebFramework.TENANT_CONTEXT_ATTRIBUTE) TenantContext context) {
        return ApiResponse.ok(new AuthorizationContextResponse(
                context.tenantId(),
                context.membershipRole(),
                context.permissions().stream().sorted().toList(),
                context.tenantPackage(),
                visibleMenus(context)));
    }

    /** 从权限目录中筛选当前上下文可见的菜单与目录。 */
    private List<ManagementPermissionView> visibleMenus(TenantContext context) {
        return catalog.activeViews().stream()
                .filter(permission -> permission.type() == ManagementPermissionType.MENU
                        || permission.type() == ManagementPermissionType.DIRECTORY)
                .filter(permission -> context.permissions().contains(permission.code()))
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
    public record AuthorizationContextResponse(
            UUID tenantId,
            String membershipRole,
            List<String> permissions,
            TenantPackageView tenantPackage,
            List<ManagementPermissionView> menus) {
    }
}
