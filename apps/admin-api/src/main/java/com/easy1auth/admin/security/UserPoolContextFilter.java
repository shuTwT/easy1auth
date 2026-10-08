package com.easy1auth.admin.security;

import com.easy1auth.framework.web.util.WebFrameworkUtils;
import com.easy1auth.framework.tenant.context.UserPoolContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 用户池上下文解析过滤器。
 *
 * <p>对需要用户池上下文的认证请求，从 {@code tenant-id} 请求头解析用户池并调用
 * {@link UserPoolContextHolder}，供领域服务读取当前用户池 ID。请求结束后恢复之前的
 * 线程上下文，避免 Servlet 工作线程复用造成用户池泄漏。</p>
 */
@Component
public final class UserPoolContextFilter extends OncePerRequestFilter {
    /**
     * 管理路由清单，用于判断当前请求是否需要用户池上下文
     */
    private final ManagementRouteInventory routes;
    public UserPoolContextFilter(ManagementRouteInventory routes) {
        this.routes = routes;
    }

    /**
     * 仅对需要用户池上下文的路由执行解析，其余请求直接放行。
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !routes.requiresUserPoolContext(request);
    }

    /**
     * 解析用户池上下文并放行请求，结束时恢复线程原有的用户池上下文。
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
        UUID previousUserPoolId = UserPoolContextHolder.getUserPoolId();
        UUID userPoolId = WebFrameworkUtils.getUserPoolId(request);
        UserPoolContextHolder.setUserPoolId(userPoolId);
        try {
            chain.doFilter(request, response);
        } finally {
            UserPoolContextHolder.setUserPoolId(previousUserPoolId);
        }
    }
}
