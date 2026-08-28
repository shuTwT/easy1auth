package com.easy1auth.tenant.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;
import java.util.UUID;

/**
 * Web 框架工具类。
 *
 * <p>提供从 {@link HttpServletRequest} 中提取租户标识（tenant-id 请求头）、
 * 用户代理（User-Agent），以及读写租户上下文的静态方法，
 * 供过滤器与控制器在请求链路中透传多租户信息。</p>
 */
public final class WebFramework {
    /** 租户标识请求头名：请求通过该请求头声明所属租户 */
    public static final String TENANT_ID_HEADER = "tenant-id";
    /** 用户代理请求头名 */
    public static final String USER_AGENT_HEADER = "User-Agent";
    /** 请求属性中存放租户上下文的键名 */
    public static final String TENANT_CONTEXT_ATTRIBUTE = "com.easy1auth.tenant.util.WebFramework.tenantContext";

    private WebFramework() {
    }

    /** 从请求头解析租户 ID；请求头缺失或空白时返回 null。 */
    public static UUID getTenantId(HttpServletRequest request) {
        String tenantId = request(request).getHeader(TENANT_ID_HEADER);
        return tenantId == null || tenantId.isBlank() ? null : UUID.fromString(tenantId);
    }

    /** 读取请求的用户代理（User-Agent）请求头，可能为 null。 */
    public static String getUserAgent(HttpServletRequest request) {
        return request(request).getHeader(USER_AGENT_HEADER);
    }

    /** 将租户上下文写入请求属性，供同一请求后续环节读取（context 不可为空）。 */
    public static void setTenantContext(HttpServletRequest request, TenantContext context) {
        request(request).setAttribute(TENANT_CONTEXT_ATTRIBUTE, Objects.requireNonNull(context, "context"));
    }

    /** 读取请求属性中的租户上下文；未设置时返回 null。 */
    public static TenantContext getTenantContext(HttpServletRequest request) {
        Object context = request(request).getAttribute(TENANT_CONTEXT_ATTRIBUTE);
        return context == null ? null : TenantContext.class.cast(context);
    }

    /** 读取租户上下文，缺失时抛出异常（用于必须处于租户上下文的场景）。 */
    public static TenantContext requireTenantContext(HttpServletRequest request) {
        return Objects.requireNonNull(getTenantContext(request), "Tenant context is unavailable");
    }

    /** 校验请求对象不为空（内部工具方法）。 */
    private static HttpServletRequest request(HttpServletRequest request) {
        return Objects.requireNonNull(request, "request");
    }
}
