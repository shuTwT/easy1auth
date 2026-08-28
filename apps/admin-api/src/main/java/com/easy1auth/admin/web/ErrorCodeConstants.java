package com.easy1auth.admin.web;

import com.easy1auth.infrastructure.foundation.error.ErrorCode;

/**
 * 管理端接口错误码常量（包私有约定）。
 *
 * <p>集中定义管理端 REST API 使用的业务错误码与中文提示，供控制器与
 * {@link GlobalExceptionHandler} 引用，保证错误码全局唯一且语义一致。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode AUTHENTICATION_SUBJECT_INVALID = new ErrorCode(23000, "认证主体无效");
    ErrorCode CODE_TYPE_UNSUPPORTED = new ErrorCode(23001, "不支持的验证码类型");
    ErrorCode EMAIL_CHANGE_CHALLENGE_INVALID = new ErrorCode(23002, "邮箱换绑挑战无效或已过期");
    ErrorCode IMPORT_FILE_INVALID = new ErrorCode(23003, "导入文件为空或超过5MB");
    ErrorCode LOGIN_TYPE_UNSUPPORTED = new ErrorCode(23004, "不支持的登录方式");
    ErrorCode MFA_CHALLENGE_INVALID = new ErrorCode(23005, "邮箱登录挑战无效或已过期");
    ErrorCode PASSWORD_CONFIRM_MISMATCH = new ErrorCode(23006, "两次输入的密码不一致");
    ErrorCode TENANT_PACKAGE_REQUIRED = new ErrorCode(23007, "租户套餐不能为空");
}
