package com.easy1auth.admin.web;

import com.easy1auth.tenant.WebFramework;

import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
import com.easy1auth.adminaccess.ManagementPermissionCatalog;
import com.easy1auth.adminaccess.ManagementPermissionType;
import com.easy1auth.adminaccess.ManagementPermissionView;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.TenantContext;
import com.easy1auth.tenant.TenantPackageView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/authorization")
public class AuthorizationContextController {
    private final ManagementPermissionCatalog catalog;

    AuthorizationContextController(ManagementPermissionCatalog catalog) {
        this.catalog = catalog;
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHORIZATION_CONTEXT)
    @GetMapping("/context")
    public ApiResponse<AuthorizationContextResponse> context(HttpServletRequest request) {
        TenantContext context = WebFramework.requireTenantContext(request);
        return ApiResponse.ok(new AuthorizationContextResponse(
                context.tenantId(),
                context.membershipRole(),
                context.permissions().stream().sorted().toList(),
                context.tenantPackage(),
                visibleMenus(context)));
    }

    private List<ManagementPermissionView> visibleMenus(TenantContext context) {
        return catalog.activeViews().stream()
                .filter(permission -> permission.type() == ManagementPermissionType.MENU
                        || permission.type() == ManagementPermissionType.DIRECTORY)
                .filter(permission -> context.permissions().contains(permission.code()))
                .toList();
    }

    public record AuthorizationContextResponse(
            UUID tenantId,
            String membershipRole,
            List<String> permissions,
            TenantPackageView tenantPackage,
            List<ManagementPermissionView> menus) {
    }
}
