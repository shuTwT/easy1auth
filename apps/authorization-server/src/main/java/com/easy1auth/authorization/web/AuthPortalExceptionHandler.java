package com.easy1auth.authorization.web;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 授权门户异常处理器。
 *
 * <p>拦截 {@link LoginController} 抛出的领域异常（{@link DomainException}），统一转换为
 * 门户 API 错误响应：走门户 API（/auth-portal-api/）时以 HTTP 200 携带业务错误码返回，
 * 其余场景（如页面请求）以 HTTP 400 返回。</p>
 */
@RestControllerAdvice(assignableTypes = LoginController.class)
public class AuthPortalExceptionHandler {
    /** 将领域异常转换为统一的 ApiError 响应体返回。 */
    @ExceptionHandler(DomainException.class)
    ResponseEntity<LoginController.ApiError> domain(DomainException ex, HttpServletRequest request) {
        var body = new LoginController.ApiError(ex.code(), ex.getMessage());
        return request.getRequestURI().startsWith("/auth-portal-api/")
                ? ResponseEntity.ok(body)
                : ResponseEntity.badRequest().body(body);
    }
}
