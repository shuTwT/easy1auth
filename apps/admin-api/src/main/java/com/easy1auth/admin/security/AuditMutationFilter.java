package com.easy1auth.admin.security;

import com.easy1auth.audit.AuditService;
import com.easy1auth.foundation.trace.TraceIdFilter;
import com.easy1auth.tenant.WebFramework;
import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public final class AuditMutationFilter extends OncePerRequestFilter {
    private final AuditService audit;

    AuditMutationFilter(AuditService audit) {
        this.audit = audit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod()) || request.getRequestURI().startsWith("/api/auth/send-code");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            try {
                var context = WebFramework.getTenantContext(request);
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                Jwt jwt = auth != null && auth.getPrincipal() instanceof Jwt j ? j : null;
                String[] path = request.getRequestURI().split("/");
                String resource = path.length > 2 ? path[2] : "api";
                audit.record(new AuditService.Event(context == null ? null : context.tenantId(), "admin", jwt == null ? null : UUID.fromString(jwt.getSubject()), null, "admin_api", request.getMethod().toLowerCase(Locale.ROOT), resource, path.length > 3 ? path[3] : null, response.getHeader(TraceIdFilter.HEADER), request.getMethod(), request.getRemoteAddr(), WebFramework.getUserAgent(request), response.getStatus() < 400 ? "success" : "failure", response.getStatus() < 400 ? null : "HTTP_" + response.getStatus(), Map.of("path", request.getRequestURI())));
            } catch (RuntimeException ignored) {
            }
        }
    }
}
