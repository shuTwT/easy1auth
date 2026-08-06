package com.easy1auth.authorization.web;

import com.easy1auth.foundation.error.DomainException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthPortalExceptionHandler {
    @ExceptionHandler(DomainException.class)
    ResponseEntity<LoginController.ApiError> domain(DomainException ex) {
        return ResponseEntity.status(ex.status()).body(new LoginController.ApiError(ex.code(), ex.getMessage()));
    }
}
