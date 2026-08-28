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

/**
 * 租户上下文解析过滤器。
 *
 * <p>对需要租户上下文的认证请求，从 {@code tenant-id} 请求头解析租户并调用
 * {@link TenantService#resolve} 校验账号在该租户的有效成员关系与角色，构建
 * {@link TenantContext} 供后续权限判断；解析失败时直接返回业务错误。请求结束后恢复
 * 之前的租户上下文，避免污染其他请求。</p>
 */
@Component
public final class TenantContextFilter extends OncePerRequestFilter {
    /** 租户服务，负责解析租户上下文与权限 */
    private final TenantService tenants;
    /** 管理路由清单，用于判断当前请求是否需要租户上下文 */
    private final ManagementRouteInventory routes;
    /** 业务错误写入器，用于以统一 JSON 格式返回租户上下文错误 */
    private final ApiErrorWriter errors;

    public TenantContextFilter(TenantService tenants, ManagementRouteInventory routes, ApiErrorWriter errors) {
        this.tenants = tenants;
        this.routes = routes;
        this.errors = errors;
    }

    /** 仅对需要租户上下文的路由执行解析，其余请求直接放行。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !routes.requiresTenantContext(request);
    }

    /** 解析租户上下文并放行请求，结束时恢复线程原有的租户上下文。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            chain.doFilter(request, response);
            return;
        }
        UUID previousTenantId = TenantContextHolder.getTenantId();
        boolean previousIgnore = TenantContextHolder.isIgnore();
        try {
            try {
                UUID tenantId = WebFramework.getTenantId(request);
                if (tenantId == null) {
                    throw new DomainException(com.easy1auth.tenant.ErrorCodeConstants.TENANT_CONTEXT_REQUIRED);
                }
                TenantContextHolder.setTenantId(tenantId);
                TenantContextHolder.setIgnore(false);
                TenantContext context = tenants.resolve(UUID.fromString(jwt.getSubject()), tenantId,
                        response.getHeader(TraceIdFilter.HEADER));
                WebFramework.setTenantContext(request, context);
            } catch (IllegalArgumentException ex) {
                writeError(request, response, new DomainException(com.easy1auth.tenant.ErrorCodeConstants.TENANT_INVALID));
                return;
            } catch (DomainException ex) {
                writeError(request, response, ex);
                return;
            }
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.setTenantId(previousTenantId);
            TenantContextHolder.setIgnore(previousIgnore);
        }
    }

    /** 以统一 JSON 格式写出租户上下文相关错误。 */
    private void writeError(HttpServletRequest request, HttpServletResponse response, DomainException ex) throws IOException {
        errors.write(request, response, ex.errorCode());
    }
}
