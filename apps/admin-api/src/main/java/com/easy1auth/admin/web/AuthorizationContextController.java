package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantContextFilter;
import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
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
    @ManagementRouteClassification(ManagementRouteKind.AUTHORIZATION_CONTEXT)
    @GetMapping("/context")
    public ApiResponse<AuthorizationContextResponse> context(HttpServletRequest request) {
        TenantContext context = (TenantContext) Objects.requireNonNull(request.getAttribute(TenantContextFilter.ATTRIBUTE));
        return ApiResponse.ok(new AuthorizationContextResponse(
                context.tenantId(),
                context.membershipRole(),
                context.permissions().stream().sorted().toList(),
                context.dataBoundary().code(),
                context.tenantPackage()));
    }

    public record AuthorizationContextResponse(
            UUID tenantId,
            String membershipRole,
            List<String> permissions,
            String dataBoundary,
            TenantPackageView tenantPackage) {
    }
}
