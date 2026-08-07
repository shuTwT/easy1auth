package com.easy1auth.federation;

import com.easy1auth.foundation.error.ErrorCode;

public interface ErrorCodeConstants {
    ErrorCode OIDC_BINDING_REQUIRED = new ErrorCode(18000, "该外部身份尚未绑定");
    ErrorCode OIDC_DISCOVERY_FAILED = new ErrorCode(18001, "无法读取 OIDC Discovery");
    ErrorCode OIDC_DISCOVERY_INVALID = new ErrorCode(18002, "OIDC Discovery issuer 不匹配");
    ErrorCode OIDC_ENDPOINT_FORBIDDEN = new ErrorCode(18003, "OIDC 地址不能指向私有网络");
    ErrorCode OIDC_ENDPOINT_INVALID = new ErrorCode(18004, "OIDC Endpoint 无效");
    ErrorCode OIDC_ID_TOKEN_INVALID = new ErrorCode(18005, "OIDC ID Token 校验失败");
    ErrorCode OIDC_ID_TOKEN_MISSING = new ErrorCode(18006, "上游未返回 ID Token");
    ErrorCode OIDC_ISSUER_INVALID = new ErrorCode(18007, "OIDC issuer 必须是 HTTPS（localhost 除外）");
    ErrorCode OIDC_PROVIDER_DISABLED = new ErrorCode(18008, "OIDC 身份源已停用");
    ErrorCode OIDC_PROVIDER_INVALID = new ErrorCode(18009, "OIDC 身份源参数不完整");
    ErrorCode OIDC_PROVIDER_NOT_FOUND = new ErrorCode(18010, "OIDC 身份源不存在");
    ErrorCode OIDC_REDIRECT_INVALID = new ErrorCode(18011, "OIDC 回调地址无效");
    ErrorCode OIDC_STATUS_INVALID = new ErrorCode(18012, "身份源状态无效");
    ErrorCode OIDC_TOKEN_EXCHANGE_FAILED = new ErrorCode(18013, "OIDC Token 交换失败");
    ErrorCode OIDC_TRANSACTION_INVALID = new ErrorCode(18014, "OIDC 登录事务无效或已过期");
    ErrorCode OIDC_VERIFIED_EMAIL_REQUIRED = new ErrorCode(18015, "JIT 创建需要已验证邮箱");
}
