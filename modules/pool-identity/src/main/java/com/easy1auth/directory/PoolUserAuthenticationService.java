package com.easy1auth.directory;

import com.easy1auth.directory.model.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import com.easy1auth.security.*;

@Service
public class PoolUserAuthenticationService {
    private static final PoolUserEntityTable USER = PoolUserEntityTable.$;
    private final JSqlClient sql;
    private final PasswordEncoder passwords;
    private final LoginProtectionService protection;
    private final SecurityPolicyService security;

    public PoolUserAuthenticationService(JSqlClient sql, PasswordEncoder passwords, LoginProtectionService protection, SecurityPolicyService security) {
        this.sql = sql;
        this.passwords = passwords;
        this.protection = protection;
        this.security = security;
    }

    @Transactional
    public PoolPrincipal authenticate(UUID tenant, String login, String password) {
        if (login == null || password == null) return null;
        String key = login.strip().toLowerCase();
        var policy = security.policy(tenant);
        protection.assertAllowed("pool_user", key, tenant);
        var user = sql.createQuery(USER).where(USER.tenantId().eq(tenant), USER.status().eq("active"), Predicate.or(USER.username().eq(login), USER.email().eq(login.toLowerCase()))).select(USER).fetchOneOrNull();
        if (user == null || user.passwordHash() == null || !passwords.matches(password, user.passwordHash())) {
            protection.failed("pool_user", key, tenant, policy.loginAttemptLimit(), policy.lockoutSeconds());
            return null;
        }
        protection.succeeded("pool_user", key, tenant);
        sql.createUpdate(USER).set(USER.lastLoginAt(), Instant.now()).set(USER.updatedAt(), Instant.now()).where(USER.id().eq(user.id()), USER.tenantId().eq(tenant)).execute();
        return new PoolPrincipal(user.id(), user.tenantId(), user.username(), user.name(), user.email());
    }

    public record PoolPrincipal(UUID id, UUID tenantId, String username, String name, @Nullable String email) {
    }
}
