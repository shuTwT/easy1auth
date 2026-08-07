package com.easy1auth.authorization.web;

import com.easy1auth.foundation.error.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = LoginController.class)
public class AuthPortalExceptionHandler {
    @ExceptionHandler(DomainException.class)
    ResponseEntity<LoginController.ApiError> domain(DomainException ex, HttpServletRequest request) {
        var body = new LoginController.ApiError(ex.code(), ex.getMessage());
        return request.getRequestURI().startsWith("/auth-portal-api/")
                ? ResponseEntity.ok(body)
                : ResponseEntity.badRequest().body(body);
    }
}
