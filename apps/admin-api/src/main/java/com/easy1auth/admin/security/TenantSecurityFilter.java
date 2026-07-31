package com.easy1auth.admin.security;

import com.easy1auth.tenant.TenantContext;
import com.easy1auth.tenant.WebFramework;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public final class TenantSecurityFilter extends OncePerRequestFilter {
    private final ManagementRouteInventory routes;
    private final ApiErrorWriter errors;

    TenantSecurityFilter(ManagementRouteInventory routes, ApiErrorWriter errors) {
        this.routes = routes;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var requirement = routes.tenantPermission(request);
        if (requirement.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            chain.doFilter(request, response);
            return;
        }

        TenantContext context = WebFramework.getTenantContext(request);
        if (context == null) {
            errors.write(response, 403, "租户上下文不可用");
            return;
        }
        if (!context.hasPermission(requirement.get().value())) {
            errors.write(response, 403, "权限不足");
            return;
        }
        chain.doFilter(request, response);
    }
}
