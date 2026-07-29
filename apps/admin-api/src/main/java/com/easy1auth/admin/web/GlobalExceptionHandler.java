package com.easy1auth.admin.web;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DomainException.class)
    ResponseEntity<?> domain(DomainException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.status()).body(Map.of(
                "status", ex.status() >= 500 ? "error" : "fail",
                "code", ex.code(), "message", ex.getMessage(), "path", request.getRequestURI()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(409).body(Map.of("status","fail","code","DATA_CONFLICT","message","数据已存在或违反关联约束","path",request.getRequestURI()));
    }
}
