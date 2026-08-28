package com.easy1auth.security;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.error.ErrorCode;

/**
 * 安全策略域错误码常量（17000-17011）。
 *
 * <p>覆盖登录锁定、验证码限流、MFA/TOTP、密码强度与复用、安全策略校验等
 * 环节的业务错误，供 {@link DomainException} 使用。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode ACCOUNT_TEMPORARILY_LOCKED = new ErrorCode(17000, "登录失败次数过多，请稍后再试");
    ErrorCode CODE_RATE_LIMITED = new ErrorCode(17001, "验证码发送过于频繁");
    ErrorCode MFA_ALREADY_ENABLED = new ErrorCode(17002, "TOTP 已启用");
    ErrorCode MFA_CHALLENGE_INVALID = new ErrorCode(17003, "MFA 挑战无效或已过期");
    ErrorCode MFA_CHALLENGE_REPLAYED = new ErrorCode(17004, "MFA 挑战已使用");
    ErrorCode MFA_CODE_INVALID = new ErrorCode(17005, "验证码无效");
    ErrorCode MFA_NOT_CONFIGURED = new ErrorCode(17006, "MFA 尚未配置");
    ErrorCode MFA_NOT_ENABLED = new ErrorCode(17007, "MFA 未启用");
    ErrorCode PASSWORD_REUSED_CURRENT = new ErrorCode(17008, "不能重复使用当前密码");
    ErrorCode PASSWORD_REUSED_RECENT = new ErrorCode(17009, "不能使用最近使用过的密码");
    ErrorCode PASSWORD_WEAK = new ErrorCode(17010, "密码不符合安全策略");
    ErrorCode SECURITY_POLICY_INVALID = new ErrorCode(17011, "安全策略参数无效");
}
