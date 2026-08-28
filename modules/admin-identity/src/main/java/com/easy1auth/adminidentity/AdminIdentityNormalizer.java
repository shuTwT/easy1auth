package com.easy1auth.adminidentity;

import java.util.Locale;

/**
 * 管理账号身份归一化工具（包私有）。
 *
 * <p>统一对用户名、邮箱、登录标识做去首尾空白与小写化处理，
 * 保证账号查询与注册判重时大小写不敏感。</p>
 */
final class AdminIdentityNormalizer {
    private AdminIdentityNormalizer() {
    }

    /** 归一化用户名与邮箱，返回组合结果。 */
    static NormalizedIdentity normalize(String username, String email) {
        return new NormalizedIdentity(normalizeUsername(username), normalizeEmail(email));
    }

    /** 归一化登录标识（去空白 + 小写）。 */
    static String normalizeLogin(String login) {
        return login == null ? null : login.strip().toLowerCase(Locale.ROOT);
    }

    /** 归一化邮箱（去空白 + 小写）。 */
    static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    /** 归一化用户名（去空白 + 小写）。 */
    private static String normalizeUsername(String username) {
        return username == null ? null : username.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * 归一化后的身份信息。
     *
     * @param username 归一化后的用户名
     * @param email    归一化后的邮箱
     */
    record NormalizedIdentity(String username, String email) {
    }
}
