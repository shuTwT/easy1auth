package com.easy1auth.oauth2;

import com.easy1auth.foundation.error.ErrorCode;

public interface ErrorCodeConstants {
    ErrorCode OAUTH_ISSUER_INVALID_FORMAT = new ErrorCode(22000, "OAuth issuer 格式无效");
    ErrorCode OAUTH_ISSUER_INVALID_TENANT = new ErrorCode(22001, "OAuth issuer 租户标识无效");
    ErrorCode OAUTH_TENANT_REQUIRED = new ErrorCode(22002, "OAuth issuer 中缺少租户");
}
