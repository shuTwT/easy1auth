package com.easy1auth.foundation.trace;

import com.easy1auth.foundation.id.UuidV7;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class TraceIdFilter implements Filter {
    public static final String HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var http = (HttpServletRequest) request;
        var traceId = http.getHeader(HEADER);
        if (traceId == null || traceId.isBlank() || traceId.length() > 128) traceId = UuidV7.randomUuid().toString();
        ((HttpServletResponse) response).setHeader(HEADER, traceId);
        try (var ignored = MDC.putCloseable("traceId", traceId)) {
            chain.doFilter(request, response);
        }
    }
}
