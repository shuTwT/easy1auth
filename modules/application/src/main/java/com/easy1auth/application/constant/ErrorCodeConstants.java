package com.easy1auth.application.constant;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.error.ErrorCode;

/**
 * OAuth2 应用域错误码常量（16000-16011）。
 *
 * <p>定义应用（OAuth 客户端）注册、更新、状态与授权类型等业务场景的错误码，
 * 供领域服务抛出 {@link DomainException} 使用。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode APPLICATION_NAME_EXISTS = new ErrorCode(16000, "应用名称已存在");
    ErrorCode APPLICATION_NAME_REQUIRED = new ErrorCode(16001, "应用名称不能为空");
    ErrorCode APPLICATION_NOT_FOUND = new ErrorCode(16002, "应用不存在");
    ErrorCode APPLICATION_STATUS_INVALID = new ErrorCode(16003, "应用状态无效");
    ErrorCode APPLICATION_TYPE_INVALID = new ErrorCode(16004, "应用类型无效");
    ErrorCode GRANT_TYPE_INVALID = new ErrorCode(16005, "授权类型无效");
    ErrorCode PKCE_REQUIRED = new ErrorCode(16006, "公共客户端必须启用 PKCE");
    ErrorCode PUBLIC_CLIENT_GRANT_INVALID = new ErrorCode(16007, "公共客户端不能使用 Client Credentials");
    ErrorCode PUBLIC_CLIENT_HAS_NO_SECRET = new ErrorCode(16008, "公共客户端不使用客户端密钥");
    ErrorCode REDIRECT_URI_INVALID = new ErrorCode(16009, "重定向 URI 必须是无 fragment 的绝对 HTTP(S) URI");
    ErrorCode TENANT_APP_LIMIT = new ErrorCode(16010, "已达到租户应用数量上限");
    ErrorCode TOKEN_LIFETIME_INVALID = new ErrorCode(16011, "Token 有效期超出允许范围");
}
