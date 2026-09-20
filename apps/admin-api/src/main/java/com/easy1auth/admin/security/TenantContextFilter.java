package com.easy1auth.admin.security;

import com.easy1auth.framework.web.util.WebFrameworkUtils;
import com.easy1auth.framework.tenant.context.TenantContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 租户上下文解析过滤器。
 *
 * <p>对需要租户上下文的认证请求，从 {@code tenant-id} 请求头解析租户并调用
 * {@link TenantContextHolder}，供领域服务读取当前租户 ID。请求结束后恢复之前的
 * 线程上下文，避免 Servlet 工作线程复用造成租户泄漏。</p>
 */
@Component
public final class TenantContextFilter extends OncePerRequestFilter {
    /**
     * 管理路由清单，用于判断当前请求是否需要租户上下文
     */
    private final ManagementRouteInventory routes;
    public TenantContextFilter(ManagementRouteInventory routes) {
        this.routes = routes;
    }

    /**
     * 仅对需要租户上下文的路由执行解析，其余请求直接放行。
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !routes.requiresTenantContext(request);
    }

    /**
     * 解析租户上下文并放行请求，结束时恢复线程原有的租户上下文。
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
        UUID previousTenantId = TenantContextHolder.getTenantId();
        UUID tenantId = WebFrameworkUtils.getTenantId(request);
        TenantContextHolder.setTenantId(tenantId);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.setTenantId(previousTenantId);
        }
    }
}
