package com.easy1auth.adminidentity.constant;

import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.error.ErrorCode;

/**
 * 管理账号身份域错误码常量（12000-12018）。
 *
 * <p>定义管理后台账号/凭证/会话/注册码等业务场景的错误码，
 * 供领域服务抛出 {@link DomainException} 使用。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode ADMINISTRATOR_ACCOUNT_NOT_ACTIVE = new ErrorCode(12000, "管理员账号不存在或未启用");
    ErrorCode ADMINISTRATOR_ACCOUNT_REQUIRED = new ErrorCode(12001, "管理员账号不能为空");
    ErrorCode ADMIN_DISABLED = new ErrorCode(12002, "账号已被禁用");
    ErrorCode ADMIN_EXISTS_EMAIL = new ErrorCode(12003, "该邮箱已被注册");
    ErrorCode ADMIN_EXISTS_USERNAME_OR_EMAIL = new ErrorCode(12004, "用户名或邮箱已被注册");
    ErrorCode ADMIN_SESSION_INVALID = new ErrorCode(12005, "管理员会话已失效");
    ErrorCode CURRENT_PASSWORD_INVALID = new ErrorCode(12006, "当前密码错误");
    ErrorCode EMAIL_INVALID = new ErrorCode(12007, "邮箱格式不正确");
    ErrorCode EMAIL_UNCHANGED = new ErrorCode(12008, "新邮箱不能与当前邮箱相同");
    ErrorCode INVALID_CREDENTIALS = new ErrorCode(12009, "用户名或密码错误");
    ErrorCode INVALID_REFRESH_TOKEN = new ErrorCode(12010, "刷新令牌无效或已失效");
    ErrorCode PASSWORD_WEAK = new ErrorCode(12011, "密码至少8位且必须包含大小写字母和数字");
    ErrorCode PHONE_INVALID = new ErrorCode(12012, "手机号格式不正确");
    ErrorCode SELF_MFA_RESET_DENIED = new ErrorCode(12013, "不能重置自己的 MFA");
    ErrorCode SELF_PASSWORD_RESET_DENIED = new ErrorCode(12014, "不能重置自己的密码");
    ErrorCode SELF_STATUS_CHANGE = new ErrorCode(12015, "不能修改自己的账号状态");
    ErrorCode STATUS_INVALID = new ErrorCode(12016, "账号状态无效");
    ErrorCode USERNAME_INVALID = new ErrorCode(12017, "用户名不能为空且不能超过100字符");
    ErrorCode VERIFICATION_CODE_INVALID = new ErrorCode(12018, "验证码无效或已过期");
}

