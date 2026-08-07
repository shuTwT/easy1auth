package com.easy1auth.admin.web;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.error.ErrorCode;
import com.easy1auth.foundation.error.ErrorCodeConstants;
import com.easy1auth.foundation.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiResponse<Void>> domain(DomainException ex, HttpServletRequest request) {
        markBusinessError(request);
        return ResponseEntity.ok(ApiResponse.error(ex.errorCode()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> conflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return business(request, ErrorCodeConstants.DATA_INTEGRITY_CONFLICT);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    ResponseEntity<ApiResponse<Void>> validation(Exception ex, HttpServletRequest request) {
        var binding = ex instanceof MethodArgumentNotValidException invalid
                ? invalid.getBindingResult() : ((BindException) ex).getBindingResult();
        var fieldError = binding.getFieldError();
        return business(request, ErrorCodeConstants.REQUEST_VALIDATION_FAILED);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiResponse<Void>> badRequest(Exception ex, HttpServletRequest request) {
        return business(request, ErrorCodeConstants.REQUEST_FORMAT_INVALID);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> forbidden(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ApiResponse.error(ErrorCodeConstants.ACCESS_DENIED));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> notFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(ApiResponse.error(ErrorCodeConstants.REQUEST_RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405).body(ApiResponse.error(ErrorCodeConstants.REQUEST_METHOD_NOT_SUPPORTED));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled request error: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(500).body(ApiResponse.error(ErrorCodeConstants.INTERNAL_SERVER_ERROR));
    }

    private static ResponseEntity<ApiResponse<Void>> business(HttpServletRequest request, ErrorCode errorCode) {
        markBusinessError(request);
        return ResponseEntity.ok(ApiResponse.error(errorCode));
    }

    private static void markBusinessError(HttpServletRequest request) {
        request.setAttribute("easy1auth.business.error", Boolean.TRUE);
    }
}
