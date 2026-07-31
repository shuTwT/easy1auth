package com.easy1auth.admin.web;

import com.easy1auth.admin.security.PlatformManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCatalog;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.adminaccess.ManagementPermissionType;
import com.easy1auth.adminaccess.ManagementPermissionView;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.TenantDataBoundary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform/menus")
public class PlatformMenuController {
    private final PlatformAuthorizationResolver platformAuthorization;
    private final ManagementPermissionCatalog catalog;

    PlatformMenuController(PlatformAuthorizationResolver platformAuthorization, ManagementPermissionCatalog catalog) {
        this.platformAuthorization = platformAuthorization;
        this.catalog = catalog;
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.MENU_MANAGEMENT_LIST, boundary = TenantDataBoundary.PLATFORM_ALL)
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

    public record MenuCatalogResponse(
            List<ManagementPermissionView> menus,
            List<ManagementPermissionView> permissions) {
    }
}
