package com.easy1auth.admin.web;

import com.easy1auth.adminaccess.ManagementPermissionCatalog;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.adminaccess.ManagementPermissionView;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;
import com.easy1auth.admin.security.PlatformManagementPermission;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.web.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.easy1auth.tenant.TenantDataBoundary;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform/permissions")
public class PlatformPermissionController {
    private final PlatformAuthorizationResolver platformAuthorization;
    private final ManagementPermissionCatalog catalog;

    PlatformPermissionController(PlatformAuthorizationResolver platformAuthorization, ManagementPermissionCatalog catalog) {
        this.platformAuthorization = platformAuthorization;
        this.catalog = catalog;
    }

    @PlatformManagementPermission(value = ManagementPermissionCode.TENANT_PACKAGE_PERMISSION_CATALOG, boundary = TenantDataBoundary.PLATFORM_ALL)
    @GetMapping
    public ApiResponse<List<ManagementPermissionView>> list(@AuthenticationPrincipal Jwt actor) {
        require(actor, ManagementPermissionCode.TENANT_PACKAGE_PERMISSION_CATALOG);
        return ApiResponse.ok(catalog.activeViews());
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
}
