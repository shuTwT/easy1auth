package com.easy1auth.enterpriseidentity.constant;

import com.easy1auth.common.foundation.error.ErrorCode;

/**
 * 企业身份源域的领域错误码常量。
 *
 * <p>统一管理本模块（enterprise-identity）抛出领域异常时使用的错误码，
 * 当前主要用于飞书企业身份源的配置校验、事件回调与同步任务。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode ENTERPRISE_IDENTITY_SOURCE_DISABLED = new ErrorCode(19000, "身份源已停用");
    ErrorCode ENTERPRISE_IDENTITY_SOURCE_INVALID = new ErrorCode(19001, "飞书身份源配置不完整");
    ErrorCode ENTERPRISE_IDENTITY_SOURCE_NOT_FOUND = new ErrorCode(19002, "企业身份源不存在");
    ErrorCode ENTERPRISE_IDENTITY_SOURCE_STATUS_INVALID = new ErrorCode(19003, "身份源状态无效");
    ErrorCode FEISHU_API_FAILED = new ErrorCode(19004, "飞书通讯录请求失败");
    ErrorCode FEISHU_EVENT_DECRYPT_FAILED = new ErrorCode(19005, "飞书事件解密失败");
    ErrorCode FEISHU_EVENT_INVALID = new ErrorCode(19006, "飞书事件缺少 event_id");
    ErrorCode FEISHU_EVENT_UNAUTHORIZED = new ErrorCode(19007, "飞书事件校验失败");
    ErrorCode FEISHU_TOKEN_FAILED = new ErrorCode(19008, "无法获取飞书 tenant_access_token");
}
