package com.easy1auth.admin.security;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public final class TenantManagementPermissionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        TenantManagementPermission requirement = AnnotatedElementUtils.findMergedAnnotation(
                method.getMethod(), TenantManagementPermission.class);
        if (requirement == null) {
            return true;
        }
        Object value = request.getAttribute(TenantContextFilter.ATTRIBUTE);
        if (!(value instanceof TenantContext context)) {
            throw denied("TENANT_CONTEXT_REQUIRED", "租户上下文不可用");
        }
        if (context.dataBoundary() != requirement.boundary()) {
            throw denied("TENANT_DATA_BOUNDARY_DENIED", "租户数据范围不足");
        }
        if (!context.hasPermission(requirement.value().value())) {
            throw denied("TENANT_PERMISSION_DENIED", "权限不足");
        }
        return true;
    }

    private static DomainException denied(String code, String message) {
        return new DomainException(code, message, 403);
    }
}
