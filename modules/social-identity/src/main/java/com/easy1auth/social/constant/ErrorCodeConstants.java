package com.easy1auth.social.constant;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.error.ErrorCode;

/**
 * 社会化身份域错误码常量（18000-18012）。
 *
 * <p>覆盖身份源管理、社交登录授权/回调、用户信息拉取与身份绑定等环节的
 * 业务错误，供 {@link DomainException} 使用。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode SOCIAL_BINDING_REQUIRED = new ErrorCode(18000, "该外部身份尚未绑定");
    ErrorCode SOCIAL_TOKEN_EXCHANGE_FAILED = new ErrorCode(18001, "社交登录 Token 交换失败");
    ErrorCode SOCIAL_USERINFO_FAILED = new ErrorCode(18002, "社交登录用户信息获取失败");
    ErrorCode SOCIAL_SOURCE_DISABLED = new ErrorCode(18003, "社会化身份源已停用");
    ErrorCode SOCIAL_SOURCE_INVALID = new ErrorCode(18004, "社会化身份源参数不完整");
    ErrorCode SOCIAL_SOURCE_NOT_FOUND = new ErrorCode(18005, "社会化身份源不存在");
    ErrorCode SOCIAL_REDIRECT_INVALID = new ErrorCode(18006, "社交登录回调地址无效");
    ErrorCode SOCIAL_STATUS_INVALID = new ErrorCode(18007, "身份源状态无效");
    ErrorCode SOCIAL_TRANSACTION_INVALID = new ErrorCode(18008, "社交登录事务无效或已过期");
    ErrorCode SOCIAL_EMAIL_REQUIRED = new ErrorCode(18009, "创建新用户需要身份源提供邮箱");
    ErrorCode SOCIAL_TYPE_NOT_SUPPORTED = new ErrorCode(18010, "不支持的社会化身份源类型");
    ErrorCode SOCIAL_API_ERROR = new ErrorCode(18011, "社交平台返回错误");
    ErrorCode SOCIAL_BINDING_CONFLICT = new ErrorCode(18012, "该外部身份已绑定其他用户");
}
