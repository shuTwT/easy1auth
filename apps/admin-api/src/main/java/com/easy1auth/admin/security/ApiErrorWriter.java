package com.easy1auth.admin.security;

import com.easy1auth.foundation.error.ErrorCode;
import com.easy1auth.foundation.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * API 统一错误写入器。
 *
 * <p>以统一 JSON 结构（{@link ApiResponse#error}）向客户端写出业务错误或传输层错误：
 * 业务错误保持 HTTP 200 并在请求属性上打业务失败标记（供审计判断），传输层错误则
 * 使用对应的 HTTP 状态码（如 401 / 403）。</p>
 */
@Component
public final class ApiErrorWriter {
    /** JSON 序列化器，用于写出错误响应体 */
    private final ObjectMapper json;

    public ApiErrorWriter(ObjectMapper json) {
        this.json = json;
    }

    /** 写出业务错误：标记请求为业务失败，并以 HTTP 200 返回错误 JSON。 */
    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
        request.setAttribute("easy1auth.business.error", Boolean.TRUE);
        response.setStatus(200);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), ApiResponse.error(errorCode));
    }

    /** 写出传输层错误：使用指定的 HTTP 状态码返回错误 JSON（如认证/授权失败）。 */
    public void writeTransport(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), ApiResponse.error(errorCode));
    }
}
