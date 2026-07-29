package com.easy1auth.admin.security;

import com.easy1auth.foundation.error.DomainException;
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
    public static final String ATTRIBUTE = TenantContext.class.getName();
    private final TenantService tenants;
    private final ApiErrorWriter errors;
    public TenantContextFilter(TenantService tenants, ApiErrorWriter errors) { this.tenants = tenants; this.errors = errors; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/admin-users") || path.startsWith("/api/admin-roles") || path.startsWith("/api/users") || path.startsWith("/api/groups") || path.startsWith("/api/positions") || path.startsWith("/api/roles") || path.startsWith("/api/permissions") || path.startsWith("/api/applications") || path.startsWith("/api/security-policy") || path.startsWith("/api/social-identity-providers") || path.startsWith("/api/external-identities") || path.startsWith("/api/brand-settings") || path.startsWith("/api/login-style") || path.startsWith("/api/custom-domains") || path.startsWith("/api/message-templates") || path.startsWith("/api/audit-logs") || path.startsWith("/api/webhooks") || path.startsWith("/api/webhook-deliveries"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) { chain.doFilter(request, response); return; }
        String selected = request.getHeader("tenant-id");
        try {
            if (selected == null) throw new DomainException("TENANT_REQUIRED", "租户信息缺失", 400);
            request.setAttribute(ATTRIBUTE, tenants.resolve(UUID.fromString(jwt.getSubject()), UUID.fromString(selected), response.getHeader("X-Trace-Id")));
        } catch (IllegalArgumentException ex) {
            writeError(response,new DomainException("TENANT_INVALID", "租户标识无效", 400)); return;
        } catch (DomainException ex) { writeError(response,ex); return; }
        chain.doFilter(request, response);
    }
    private void writeError(HttpServletResponse response,DomainException ex)throws IOException{errors.write(response,ex.status(),ex.getMessage());}
}
