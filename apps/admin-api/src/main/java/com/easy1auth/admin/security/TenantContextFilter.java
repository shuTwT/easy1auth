package com.easy1auth.admin.security;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.trace.TraceIdFilter;
import com.easy1auth.tenant.WebFramework;
import com.easy1auth.tenant.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public final class TenantContextFilter extends OncePerRequestFilter {
    private final TenantService tenants;
    private final ManagementRouteInventory routes;
    private final ApiErrorWriter errors;

    public TenantContextFilter(TenantService tenants, ManagementRouteInventory routes, ApiErrorWriter errors) {
        this.tenants = tenants;
        this.routes = routes;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !routes.requiresTenantContext(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            chain.doFilter(request, response);
            return;
        }
        UUID tenantId = WebFramework.getTenantId(request);
        TenantContext context;
        try {
            if (tenantId == null) throw new DomainException("TENANT_CONTEXT_REQUIRED", "租户上下文不可用", 403);
            context = tenants.resolve(UUID.fromString(jwt.getSubject()), tenantId,
                    response.getHeader(TraceIdFilter.HEADER));
            WebFramework.setTenantContext(request, context);
        } catch (IllegalArgumentException ex) {
            writeError(response, new DomainException("TENANT_INVALID", "租户标识无效", 400));
            return;
        } catch (DomainException ex) {
            writeError(response, ex);
            return;
        }
        UUID previousTenantId = TenantContextHolder.getTenantId();
        boolean previousIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setTenantId(context.tenantId());
        TenantContextHolder.setIgnore(false);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.setTenantId(previousTenantId);
            TenantContextHolder.setIgnore(previousIgnore);
        }
    }

    private void writeError(HttpServletResponse response, DomainException ex) throws IOException {
        errors.write(response, ex.status(), ex.getMessage());
    }
}
