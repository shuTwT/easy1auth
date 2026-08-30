package com.easy1auth.infrastructure.foundation.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;
import java.util.UUID;

/**
 * Web 框架工具类。
 *
 * <p>提供从 {@link HttpServletRequest} 中提取租户标识（tenant-id 请求头）、
 * 用户代理（User-Agent），以及读写租户上下文的静态方法，
 * 供过滤器与控制器在请求链路中透传多租户信息。</p>
 */
public final class WebFrameworkUtils {

    public static final String REQUEST_ATTRIBUTE_LOGIN_USER_ID = "login_user_id";
    /** 租户标识请求头名：请求通过该请求头声明所属租户 */
    public static final String TENANT_ID_HEADER = "tenant-id";
    /** 用户代理请求头名 */
    public static final String USER_AGENT_HEADER = "User-Agent";
    /** 请求属性中存放租户上下文的键名 */
    public static final String TENANT_CONTEXT_ATTRIBUTE = "com.easy1auth.tenant.util.WebFramework.tenantContext";

    private WebFrameworkUtils() {
    }

    /** 从请求头解析租户 ID；请求头缺失或空白时返回 null。 */
    public static UUID getTenantId(HttpServletRequest request) {
        String tenantId = request(request).getHeader(TENANT_ID_HEADER);
        return tenantId == null || tenantId.isBlank() ? null : UUID.fromString(tenantId);
    }

    public static void setLoginUserId(HttpServletRequest request, UUID userId) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID, userId);
    }

    /**
     * 从请求中获取当前用户编号
     */
    public static UUID getLoginUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return (UUID) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID);
    }

    /** 读取请求的用户代理（User-Agent）请求头，可能为 null。 */
    public static String getUserAgent(HttpServletRequest request) {
        return request(request).getHeader(USER_AGENT_HEADER);
    }

    /** 校验请求对象不为空（内部工具方法）。 */
    private static HttpServletRequest request(HttpServletRequest request) {
        return Objects.requireNonNull(request, "request");
    }

    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest();
    }
}
