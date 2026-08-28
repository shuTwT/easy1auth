package com.easy1auth.poolidentity.service;

import com.easy1auth.poolidentity.model.PoolUserEntityTable;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.easy1auth.security.*;

/**
 * pool_user 认证服务：为第三方授权登录场景提供用户身份认证。
 *
 * <p>通过用户名或邮箱在指定租户下校验密码，并联动登录保护（
 * {@link LoginProtectionService}）与安全策略（{@link SecurityPolicyService}），
 * 认证成功时记录最后登录时间并返回 {@link PoolPrincipal} 身份凭据。
 * 该体系仅用于第三方授权登录，Token 不得与管理端（admin_user）混用。</p>
 */
@Service
public class PoolUserAuthenticationService {
    /** pool_user 用户表静态描述符 */
    private static final PoolUserEntityTable USER = PoolUserEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;
    /** 密码编码器（BCrypt 等，用于校验密码哈希） */
    private final PasswordEncoder passwords;
    /** 登录保护服务（失败次数、锁定与放行判定） */
    private final LoginProtectionService protection;
    /** 安全策略服务（登录尝试次数、锁定时间等租户策略） */
    private final SecurityPolicyService security;

    public PoolUserAuthenticationService(JSqlClient sql, PasswordEncoder passwords, LoginProtectionService protection, SecurityPolicyService security) {
        this.sql = sql;
        this.passwords = passwords;
        this.protection = protection;
        this.security = security;
    }

    /**
     * 校验指定租户下 pool_user 的账号密码（用户名或邮箱 + 密码）。
     *
     * <p>登录凭证缺失、用户不存在或密码不匹配时返回 null（不区分具体原因），
     * 并记录一次失败的登录尝试；认证成功时重置保护计数并刷新最后登录时间。</p>
     *
     * @param tenant   租户 ID
     * @param login    登录标识（用户名或邮箱，不区分大小写）
     * @param password 明文密码
     * @return 认证成功返回 {@link PoolPrincipal}，失败返回 null
     */
    @Transactional
    public PoolPrincipal authenticate(UUID tenant, String login, String password) {
        if (login == null || password == null) {
            return null;
        }
        String key = login.strip().toLowerCase(Locale.ROOT);
        var policy = security.policy(tenant);
        protection.assertAllowed("pool_user", key, tenant);
        var user = sql.createQuery(USER)
                .where(USER.tenantId().eq(tenant), USER.status().eq("active"),
                        Predicate.or(USER.username().lower().eq(key), USER.email().lower().eq(key)))
                .select(USER)
                .fetchOneOrNull();
        if (user == null || user.passwordHash() == null || !passwords.matches(password, user.passwordHash())) {
            protection.failed("pool_user", key, tenant, policy.loginAttemptLimit(), policy.lockoutSeconds());
            return null;
        }
        protection.succeeded("pool_user", key, tenant);
        sql.createUpdate(USER).set(USER.lastLoginAt(), Instant.now()).set(USER.updatedAt(), Instant.now()).where(USER.id().eq(user.id()), USER.tenantId().eq(tenant)).execute();
        return new PoolPrincipal(user.id(), user.tenantId(), user.username(), user.name(), user.email());
    }

    /**
     * 认证通过后的 pool_user 身份凭据。
     *
     * @param id       用户 ID
     * @param tenantId 所属租户 ID
     * @param username 用户名
     * @param name     用户姓名/显示名
     * @param email    邮箱（可为 null）
     */
    public record PoolPrincipal(UUID id, UUID tenantId, String username, String name, @Nullable String email) {
    }
}
