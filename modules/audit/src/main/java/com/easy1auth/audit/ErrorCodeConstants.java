package com.easy1auth.audit;

import com.easy1auth.foundation.error.ErrorCode;

/**
 * 审计与投递域（audit）错误码常量。
 *
 * <p>错误码统一以 21xxx 为前缀，定义审计事件、Webhook 订阅与投递相关的领域
 * 异常消息。各常量由 {@link ErrorCode} 承载编码与中文提示，供抛出
 * {@link com.easy1auth.foundation.error.DomainException} 时使用。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode AUDIT_EVENT_NOT_FOUND = new ErrorCode(21000, "审计事件不存在");
    ErrorCode AUDIT_RETENTION_INVALID = new ErrorCode(21001, "审计保留期不能少于30天");
    ErrorCode WEBHOOK_INVALID = new ErrorCode(21002, "Webhook 参数不完整");
    ErrorCode WEBHOOK_NOT_FOUND = new ErrorCode(21003, "Webhook 不存在");
    ErrorCode WEBHOOK_RETRY_INVALID = new ErrorCode(21004, "Webhook 重试次数无效");
    ErrorCode WEBHOOK_STATUS_INVALID = new ErrorCode(21005, "Webhook 状态无效");
    ErrorCode WEBHOOK_URL_FORBIDDEN = new ErrorCode(21006, "Webhook 必须使用可公开访问的 HTTPS 地址");
}
