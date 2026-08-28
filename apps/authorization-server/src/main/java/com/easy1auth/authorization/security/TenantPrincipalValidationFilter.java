package com.easy1auth.authorization.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户主体一致性校验过滤器。
 *
 * <p>对授权端点请求，校验已认证的 pool_user 主体所属租户与请求路径中的租户一致，
 * 防止已登录用户越权访问其他租户的授权流程；不匹配时返回 403 错误。</p>
 */
@Component
public class TenantPrincipalValidationFilter extends OncePerRequestFilter {
    /** 仅对授权端点（以 /oauth2/authorize 结尾）执行校验。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().endsWith("/oauth2/authorize");
    }

    /** 校验认证主体持有的租户权限与请求路径租户一致，不一致则返回 403。 */
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
