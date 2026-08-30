package com.easy1auth.admin.web;

import com.easy1auth.admin.constant.ErrorCodeConstants;
import com.easy1auth.admin.web.dto.MenuCatalogResponse;
import com.easy1auth.admin.annotation.PlatformManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCatalog;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.adminaccess.constant.ManagementPermissionType;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.web.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 平台菜单目录接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/platform/menus}，返回当前账号在平台
 * 管理侧可见的菜单与全部权限目录。操作通过 {@link PlatformManagementPermission}
 * 做平台级权限控制，并以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/platform/menus")
public class PlatformMenuController {
    /** 平台级授权解析器 */
    private final PlatformAuthorizationResolver platformAuthorization;
    /** 管理权限目录（菜单 / 目录 / 权限的定义集合） */
    private final ManagementPermissionCatalog catalog;

    PlatformMenuController(PlatformAuthorizationResolver platformAuthorization, ManagementPermissionCatalog catalog) {
        this.platformAuthorization = platformAuthorization;
        this.catalog = catalog;
    }

    /** 查询平台管理菜单（菜单与目录）及完整权限目录。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.MENU_MANAGEMENT_LIST)
    @GetMapping
    public ApiResponse<MenuCatalogResponse> list(@AuthenticationPrincipal Jwt actor) {
        platformAuthorization.require(accountId(actor), ManagementPermissionCode.MENU_MANAGEMENT_LIST);
        var permissions = catalog.activeViews();
        var menus = permissions.stream()
                .filter(permission -> permission.type() == ManagementPermissionType.MENU
                        || permission.type() == ManagementPermissionType.DIRECTORY)
                .toList();
        return ApiResponse.ok(new MenuCatalogResponse(menus, permissions));
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
     * 菜单目录响应。
     *
     * @param menus       当前可展示的菜单与目录（按权限类型过滤）
     * @param permissions 完整的权限目录视图
     */
}
