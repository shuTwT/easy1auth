package com.easy1auth.adminidentity;

/**
 * 登录成功结果：返回账号信息与签发的刷新令牌。
 *
 * @param account      已认证的管理账号
 * @param refreshToken 新签发的刷新令牌（明文，仅本次返回）
 */
public record AuthenticatedAdmin(AdminAccount account, String refreshToken) {
}
