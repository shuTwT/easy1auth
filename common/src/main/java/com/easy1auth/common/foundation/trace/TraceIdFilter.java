package com.easy1auth.common.foundation.trace;

import com.easy1auth.common.foundation.id.UuidV7;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 链路追踪 ID 过滤器：为每个请求生成或透传 Trace ID。
 *
 * <p>优先沿用请求头 {@code X-Trace-Id} 中的值（最长 128 字符），缺失或非法时
 * 生成新的 {@link UuidV7}；将 Trace ID 写入响应头并放入 SLF4J MDC，
 * 便于全链路日志串联。以最高优先级最先执行。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class TraceIdFilter implements Filter {
    /** 链路追踪 ID 的请求/响应头名称 */
    public static final String HEADER = "X-Trace-Id";

    /** 解析或生成 Trace ID，写入响应头与 MDC 后放行请求链。 */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var http = (HttpServletRequest) request;
        var traceId = http.getHeader(HEADER);
        if (traceId == null || traceId.isBlank() || traceId.length() > 128) {
            traceId = UuidV7.randomUuid().toString();
        }
        ((HttpServletResponse) response).setHeader(HEADER, traceId);
        try (var ignored = MDC.putCloseable("traceId", traceId)) {
            chain.doFilter(request, response);
        }
    }
}
