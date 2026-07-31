package com.easy1auth.tenant;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;
import java.util.UUID;

public final class WebFramework {
    public static final String TENANT_ID_HEADER = "tenant-id";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String TENANT_CONTEXT_ATTRIBUTE = "com.easy1auth.tenant.WebFramework.tenantContext";

    private WebFramework() {
    }

    public static UUID getTenantId(HttpServletRequest request) {
        String tenantId = request(request).getHeader(TENANT_ID_HEADER);
        return tenantId == null || tenantId.isBlank() ? null : UUID.fromString(tenantId);
    }

    public static String getUserAgent(HttpServletRequest request) {
        return request(request).getHeader(USER_AGENT_HEADER);
    }

    public static void setTenantContext(HttpServletRequest request, TenantContext context) {
        request(request).setAttribute(TENANT_CONTEXT_ATTRIBUTE, Objects.requireNonNull(context, "context"));
    }

    public static TenantContext getTenantContext(HttpServletRequest request) {
        Object context = request(request).getAttribute(TENANT_CONTEXT_ATTRIBUTE);
        return context == null ? null : TenantContext.class.cast(context);
    }

    public static TenantContext requireTenantContext(HttpServletRequest request) {
        return Objects.requireNonNull(getTenantContext(request), "Tenant context is unavailable");
    }

    private static HttpServletRequest request(HttpServletRequest request) {
        return Objects.requireNonNull(request, "request");
    }
}
