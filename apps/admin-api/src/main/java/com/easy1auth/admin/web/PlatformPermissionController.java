package com.easy1auth.admin.web;

import com.easy1auth.admin.constant.ErrorCodeConstants;
import com.easy1auth.adminaccess.ManagementPermissionCatalog;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.adminaccess.dto.ManagementPermissionView;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.admin.annotation.PlatformManagementPermission;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 平台权限目录接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/platform/permissions}，返回启用中的完整
 * 管理权限目录，供租户套餐权限配置等场景使用。操作通过
 * {@link PlatformManagementPermission} 做平台级权限控制，并以 {@link ApiResponse}
 * 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/platform/permissions")
public class PlatformPermissionController {
    /** 平台级授权解析器 */
    private final PlatformAuthorizationResolver platformAuthorization;
    /** 管理权限目录 */
    private final ManagementPermissionCatalog catalog;

    PlatformPermissionController(PlatformAuthorizationResolver platformAuthorization, ManagementPermissionCatalog catalog) {
        this.platformAuthorization = platformAuthorization;
        this.catalog = catalog;
    }

    /** 查询启用中的全部管理权限目录。 */
    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_PERMISSION_CATALOG)
    @GetMapping
    public ApiResponse<List<ManagementPermissionView>> list(@AuthenticationPrincipal Jwt actor) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_PERMISSION_CATALOG);
        return ApiResponse.ok(catalog.activeViews());
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
}
