package com.easy1auth.oauth2.constant;

import com.easy1auth.framework.common.error.ErrorCode;

/** OAuth2 授权服务相关的业务错误码常量。 */
public interface ErrorCodeConstants {
    /** OAuth issuer 格式无效（路径结构不符合约定） */
    ErrorCode OAUTH_ISSUER_INVALID_FORMAT = new ErrorCode(22000, "OAuth issuer 格式无效");
    /** OAuth issuer 中的用户池标识不是合法 UUID */
    ErrorCode OAUTH_ISSUER_INVALID_USER_POOL = new ErrorCode(22001, "OAuth issuer 用户池标识无效");
    /** OAuth issuer 中缺少用户池标识 */
    ErrorCode OAUTH_USER_POOL_REQUIRED = new ErrorCode(22002, "OAuth issuer 中缺少用户池");
}
