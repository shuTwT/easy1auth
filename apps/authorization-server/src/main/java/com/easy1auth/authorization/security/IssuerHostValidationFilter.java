package com.easy1auth.authorization.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Component
public class IssuerHostValidationFilter extends OncePerRequestFilter {
    private final URI allowed;

    public IssuerHostValidationFilter(@Value("${easy1auth.oauth2.issuer-base}") String issuerBase) {
        this.allowed = URI.create(issuerBase);
        if (allowed.getScheme() == null || allowed.getHost() == null || allowed.getPath() != null && !allowed.getPath().isBlank() && !"/".equals(allowed.getPath()))
            throw new IllegalStateException("OAUTH2_ISSUER_BASE must be an absolute origin without a path");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/t/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String[] parts = request.getRequestURI().split("/");
        boolean tenantValid = parts.length > 2;
        try {
            if (tenantValid) UUID.fromString(parts[2]);
        } catch (IllegalArgumentException ex) {
            tenantValid = false;
        }
        int expectedPort = allowed.getPort() < 0 ? ("https".equalsIgnoreCase(allowed.getScheme()) ? 443 : 80) : allowed.getPort();
        int requestPort = request.getServerPort();
        if (!tenantValid || !allowed.getScheme().equalsIgnoreCase(request.getScheme()) || !allowed.getHost().equalsIgnoreCase(request.getServerName()) || expectedPort != requestPort) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"invalid_tenant_issuer\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
