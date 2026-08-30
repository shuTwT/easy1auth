package com.easy1auth.admin.security;

import com.easy1auth.audit.service.AuditService;
import com.easy1auth.audit.dto.AuditEvent;
import com.easy1auth.common.foundation.trace.TraceIdFilter;
import com.easy1auth.common.foundation.util.WebFrameworkUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * 变更操作审计过滤器。
 *
 * <p>对写请求（POST / PUT / PATCH / DELETE）在请求结束后记录一条管理端操作审计事件，
 * 携带租户、操作账号、资源路径、结果与错误原因等信息，用于审计追踪。审计记录自身
 * 的异常会被吞掉，避免影响业务请求。</p>
 */
@Component
public final class AuditMutationFilter extends OncePerRequestFilter {
    /**
     * 审计服务，负责落库审计事件
     */
    private final AuditService audit;

    AuditMutationFilter(AuditService audit) {
        this.audit = audit;
    }

    /**
     * 仅对写方法执行审计；跳过发送验证码等高频或敏感写接口。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod()) || request.getRequestURI().startsWith("/api/auth/send-code");
    }

    /**
     * 放行请求链后，收集请求上下文与结果并记录一条审计事件。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            try {
                var tenantId = WebFrameworkUtils.getTenantId(request);
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                Jwt jwt = auth != null && auth.getPrincipal() instanceof Jwt j ? j : null;
                String[] path = request.getRequestURI().split("/");
                String resource = path.length > 2 ? path[2] : "api";
                boolean businessError = Boolean.TRUE.equals(request.getAttribute("easy1auth.business.error"));
                audit.record(new AuditEvent(tenantId, "admin", jwt == null ? null : UUID.fromString(jwt.getSubject()), null, "admin_api", request.getMethod().toLowerCase(Locale.ROOT), resource, path.length > 3 ? path[3] : null, response.getHeader(TraceIdFilter.HEADER), request.getMethod(), request.getRemoteAddr(), WebFrameworkUtils.getUserAgent(request), !businessError && response.getStatus() < 400 ? "success" : "failure", businessError ? "BUSINESS_ERROR" : (response.getStatus() < 400 ? null : "HTTP_" + response.getStatus()), Map.of("path", request.getRequestURI())));
            } catch (RuntimeException ignored) {
            }
        }
    }
}
