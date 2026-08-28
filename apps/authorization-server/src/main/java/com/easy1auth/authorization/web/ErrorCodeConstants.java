package com.easy1auth.authorization.web;

import com.easy1auth.foundation.error.ErrorCode;

/**
 * 授权门户业务错误码常量。
 *
 * <p>集中定义登录门户、OAuth 授权交互、MFA 与注册等场景下的错误码（编号区间
 * 24000-24099），与错误码配套的中文提示信息可直接展示给前端用户。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode AUTH_ACTION_INVALID = new ErrorCode(24000, "授权操作无效");
    ErrorCode AUTH_INPUT_INVALID = new ErrorCode(24001, "授权操作无效");
    ErrorCode AUTH_INTERACTION_EXPIRED_LOGIN = new ErrorCode(24002, "登录请求已过期，请重新发起授权");
    ErrorCode AUTH_INTERACTION_EXPIRED_PROCESSED = new ErrorCode(24003, "授权请求已过期或已处理");
    ErrorCode AUTH_INTERACTION_EXPIRED_REQUEST = new ErrorCode(24004, "授权请求已过期，请重新发起");
    ErrorCode AUTH_INTERACTION_MISMATCH_CLIENT_TENANT = new ErrorCode(24005, "授权客户端与租户不匹配");
    ErrorCode AUTH_INTERACTION_MISMATCH_REQUEST = new ErrorCode(24006, "授权请求已失效，请重新发起");
    ErrorCode LOGIN_FAILED = new ErrorCode(24007, "用户名或密码错误");
    ErrorCode LOGIN_INPUT_INVALID = new ErrorCode(24008, "请输入邮箱和密码");
    ErrorCode MFA_EXPIRED = new ErrorCode(24009, "多因素认证已过期，请重新登录");
    ErrorCode MFA_FAILED = new ErrorCode(24010, "动态验证码无效或已过期");
    ErrorCode MFA_INPUT_INVALID = new ErrorCode(24011, "请输入动态验证码");
    ErrorCode REGISTRATION_INPUT_INVALID = new ErrorCode(24012, "请输入邮箱");
    ErrorCode REGISTRATION_DISABLED = new ErrorCode(24013, "该租户未开放注册");
    ErrorCode REGISTRATION_EMAIL_EXISTS = new ErrorCode(24014, "该邮箱已注册");
    ErrorCode VERIFICATION_CODE_INVALID = new ErrorCode(24015, "验证码无效或已过期");
}
