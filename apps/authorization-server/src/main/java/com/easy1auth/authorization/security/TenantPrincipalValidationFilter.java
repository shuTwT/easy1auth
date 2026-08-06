package com.easy1auth.authorization.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantPrincipalValidationFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().endsWith("/oauth2/authorize");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_POOL_USER".equals(a.getAuthority()))) {
            String[] parts = request.getRequestURI().split("/");
            String required = parts.length > 2 ? "TENANT_" + parts[2] : "";
            if (authentication.getAuthorities().stream().noneMatch(a -> required.equals(a.getAuthority()))) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"tenant_principal_mismatch\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
