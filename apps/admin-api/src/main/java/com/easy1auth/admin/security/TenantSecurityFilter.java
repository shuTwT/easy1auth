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

/**
 * 租户安全校验过滤器。
 *
 * <p>对声明了 {@link TenantManagementPermission} 的路由，基于已解析的
 * {@link TenantContext} 校验当前账号是否具备所要求的租户级权限；无权限、无租户上下文
 * 或未认证时返回相应业务错误，防止越权访问。</p>
 */
@Component
public final class TenantSecurityFilter extends OncePerRequestFilter {
    /** 管理路由清单，用于查询当前请求要求的租户权限 */
    private final ManagementRouteInventory routes;
    /** 业务错误写入器，用于统一返回权限错误 */
    private final ApiErrorWriter errors;

    TenantSecurityFilter(ManagementRouteInventory routes, ApiErrorWriter errors) {
        this.routes = routes;
        this.errors = errors;
    }

    /** 校验当前账号在租户上下文中的权限，通过后放行请求。 */
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
            errors.write(request, response, com.easy1auth.tenant.ErrorCodeConstants.TENANT_CONTEXT_REQUIRED);
            return;
        }
        if (!context.hasPermission(requirement.get().value())) {
            errors.write(request, response, com.easy1auth.adminaccess.ErrorCodeConstants.PERMISSION_DENIED);
            return;
        }
        chain.doFilter(request, response);
    }
}
