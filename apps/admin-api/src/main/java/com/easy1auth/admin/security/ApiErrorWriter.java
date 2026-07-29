package com.easy1auth.admin.security;

import com.easy1auth.foundation.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public final class ApiErrorWriter {
    private final ObjectMapper json;

    public ApiErrorWriter(ObjectMapper json) {
        this.json = json;
    }

    public void write(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), ApiResponse.error(msg));
    }
}
