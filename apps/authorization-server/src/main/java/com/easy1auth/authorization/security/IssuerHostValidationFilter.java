package com.easy1auth.authorization.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Issuer 主机校验过滤器。
 *
 * <p>对形如 {@code /t/{tenant}/...} 的多租户路径请求，校验请求的主机、协议、端口与
 * 配置的 issuer 一致，并确认路径中的租户段为合法 UUID，防止跨租户伪冒 issuer 访问
 * 授权端点。校验失败时返回 400 错误。</p>
 */
@Component
public class IssuerHostValidationFilter extends OncePerRequestFilter {
    /** 配置允许的 issuer 源（协议 + 主机 + 端口） */
    private final URI allowed;

    public IssuerHostValidationFilter(@Value("${easy1auth.oauth2.issuer-base}") String issuerBase) {
        this.allowed = URI.create(issuerBase);
        if (allowed.getScheme() == null || allowed.getHost() == null || allowed.getPath() != null && !allowed.getPath().isBlank() && !"/".equals(allowed.getPath())) {
            throw new IllegalStateException("OAUTH2_ISSUER_BASE must be an absolute origin without a path");
        }
    }

    /** 仅对多租户路径（以 /t/ 开头）执行校验，其余请求直接放行。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/t/");
    }

    /** 校验租户段合法性及请求主机与配置 issuer 一致，不匹配则返回 400。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String[] parts = request.getRequestURI().split("/");
        boolean tenantValid = parts.length > 2;
        try {
            if (tenantValid) {
                UUID.fromString(parts[2]);
            }
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
