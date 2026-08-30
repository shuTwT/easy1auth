package com.easy1auth.admin.config;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.error.ErrorCode;
import com.easy1auth.infrastructure.foundation.error.ErrorCodeConstants;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
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

/**
 * 管理端全局异常处理器。
 *
 * <p>通过 {@code @RestControllerAdvice} 统一捕获管理端 REST API 抛出的各类异常，
 * 转换成规范的 {@link ApiResponse} 错误响应与合适的 HTTP 状态码，避免异常细节
 * 直接暴露给前端。业务类异常标记为业务错误以便与系统错误区分统计。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 处理业务领域异常，返回其错误码对应的响应。 */
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiResponse<Void>> domain(DomainException ex, HttpServletRequest request) {
        markBusinessError(request);
        return ResponseEntity.ok(ApiResponse.error(ex.errorCode()));
    }

    /** 处理数据完整性冲突（如唯一键重复、外键约束失败）。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> conflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return business(request, ErrorCodeConstants.DATA_INTEGRITY_CONFLICT);
    }

    /** 处理参数校验失败（@Valid / 数据绑定错误）。 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    ResponseEntity<ApiResponse<Void>> validation(Exception ex, HttpServletRequest request) {
        var binding = ex instanceof MethodArgumentNotValidException invalid
                ? invalid.getBindingResult() : ((BindException) ex).getBindingResult();
        var fieldError = binding.getFieldError();
        return business(request, ErrorCodeConstants.REQUEST_VALIDATION_FAILED);
    }

    /** 处理请求体不可读、缺少参数、参数类型不匹配等格式错误。 */
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiResponse<Void>> badRequest(Exception ex, HttpServletRequest request) {
        return business(request, ErrorCodeConstants.REQUEST_FORMAT_INVALID);
    }

    /** 处理权限不足（403 Forbidden）。 */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> forbidden(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ApiResponse.error(ErrorCodeConstants.ACCESS_DENIED));
    }

    /** 处理请求资源不存在（404 Not Found）。 */
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> notFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(ApiResponse.error(ErrorCodeConstants.REQUEST_RESOURCE_NOT_FOUND));
    }

    /** 处理不支持的请求方法（405 Method Not Allowed）。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405).body(ApiResponse.error(ErrorCodeConstants.REQUEST_METHOD_NOT_SUPPORTED));
    }

    /** 兜底处理未捕获的其它异常，记录日志并返回 500。 */
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
