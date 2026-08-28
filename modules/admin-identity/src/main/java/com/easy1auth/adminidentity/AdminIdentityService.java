package com.easy1auth.adminidentity;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.foundation.security.ActiveAdminAccountLocker;
import com.easy1auth.security.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.*;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * 管理后台账号身份服务。
 *
 * <p>覆盖管理账号（admin_user）的认证登录、自助注册、管理员创建、刷新令牌的
 * 签发/轮换/吊销，以及账号资料、密码、MFA、状态管理等业务能力；同时实现
 * {@link ActiveAdminAccountLocker}，为跨域并发操作提供账号锁定。</p>
 */
@Service
public class AdminIdentityService implements ActiveAdminAccountLocker {
    /** 安全随机数生成器（生成 32 字节刷新令牌） */
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 管理账号身份数据访问仓储 */
    private final AdminIdentityRepository repository;
    /** 密码编码器（BCrypt） */
    private final PasswordEncoder passwords;
    /** 登录防护服务（失败次数统计与锁定） */
    private final LoginProtectionService protection;
    /** 安全策略服务（密码强度与历史密码校验） */
    private final SecurityPolicyService security;

    AdminIdentityService(AdminIdentityRepository repository, PasswordEncoder passwords, LoginProtectionService protection, SecurityPolicyService security) {
        this.repository = repository;
        this.passwords = passwords;
        this.protection = protection;
        this.security = security;
    }

    /**
     * 管理账号密码登录：校验登录标识与密码（含登录防护与账号状态检查），
     * 成功后记录登录时间并签发刷新令牌。
     */
    @Transactional
    public AuthenticatedAdmin authenticate(String login, String password, Duration refreshTtl, String userAgent, String ip) {
        if (login == null || password == null) {
            throw invalidCredentials();
        }
        String key = AdminIdentityNormalizer.normalizeLogin(login);
        protection.assertAllowed("admin", key, null);
        var found = repository.findCredential(key);
        if (found.isEmpty()) {
            protection.failed("admin", key, null, 5, 1800);
            throw invalidCredentials();
        }
        var row = found.get();
        if (!"active".equals(row.account().status())) {
            throw new DomainException(ErrorCodeConstants.ADMIN_DISABLED);
        }
        if (!passwords.matches(password, row.passwordHash())) {
            protection.failed("admin", key, null, 5, 1800);
            throw invalidCredentials();
        }
        protection.succeeded("admin", key, null);
        repository.recordLogin(row.account().id());
        return new AuthenticatedAdmin(row.account(), issueRefresh(row.account(), refreshTtl, userAgent, ip).token());
    }

    /** 管理账号自助注册：校验邮箱验证码并创建账号，成功后签发刷新令牌。 */
    @Transactional
    public AuthenticatedAdmin register(String username, String email, String password, String code, Duration refreshTtl, String userAgent, String ip) {
        var identity = AdminIdentityNormalizer.normalize(username, email);
        validateCredentials(identity.username(), identity.email(), password);
        if (code == null || code.isBlank() || !repository.consumeRegistrationCode(identity.email(), TokenHash.sha256(code))) {
            throw new DomainException(ErrorCodeConstants.VERIFICATION_CODE_INVALID);
        }
        if (repository.exists(identity.username(), identity.email())) {
            throw adminExists();
        }
        var account = createAccount(identity, password);
        return new AuthenticatedAdmin(account, issueRefresh(account, refreshTtl, userAgent, ip).token());
    }

    /** 平台侧创建管理账号（无需注册码，直接设置密码）。 */
    @Transactional
    public AdminAccount createAdministrator(String username, String email, String password) {
        var identity = AdminIdentityNormalizer.normalize(username, email);
        validateCredentials(identity.username(), identity.email(), password);
        if (repository.exists(identity.username(), identity.email())) {
            throw adminExists();
        }
        return createAccount(identity, password);
    }

    /** 校验令牌携带的账号 ID 与安全版本号是否仍有效（用于访问令牌的 subject 校验）。 */
    @Transactional(readOnly = true)
    public AdminAccount validateTokenSubject(UUID accountId, long securityVersion) {
        var account = repository.findActive(accountId).orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_SESSION_INVALID));
        if (account.securityVersion() != securityVersion) {
            throw new DomainException(ErrorCodeConstants.ADMIN_SESSION_INVALID);
        }
        return account;
    }

    /** 轮换刷新令牌：校验旧令牌有效性与账号安全版本号，签发新令牌并吊销旧会话。 */
    @Transactional
    public RefreshSession rotate(String token, Duration ttl, String userAgent, String ip) {
        if (token == null || token.isBlank()) {
            throw invalidRefresh();
        }
        var old = repository.lockSession(TokenHash.sha256(token)).orElseThrow(AdminIdentityService::invalidRefresh);
        var account = old.account();
        if (!"active".equals(account.status()) || account.securityVersion() != old.sessionSecurityVersion()) {
            throw invalidRefresh();
        }
        var replacement = issueRefresh(account, ttl, userAgent, ip);
        repository.rotate(old.id(), replacement.id());
        return new RefreshSession(old.id(), account, replacement.token());
    }

    /** 退出登录：吊销指定刷新令牌对应的会话。 */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            repository.revoke(TokenHash.sha256(refreshToken));
        }
    }

    /** 退出该账号的全部登录会话。 */
    @Transactional
    public void logoutAll(UUID accountId) {
        repository.revokeAll(accountId);
    }

    /** 注销该账号的全部会话并递增安全版本号（使所有已签发令牌失效）。 */
    @Transactional
    public void logoutAllAndInvalidate(UUID accountId) {
        repository.invalidateAccountSessions(accountId);
    }

    /** 更新指定账号的资料（用户名/邮箱/手机号，仅更新非空字段）。 */
    @Transactional
    public AdminAccount updateProfile(UUID id, String username, String email, String phone) {
        return repository.updateProfile(id, username, email, phone);
    }

    /** 更新当前账号自己的用户名与手机号（含格式校验，处理唯一性冲突）。 */
    @Transactional
    public AdminAccount updateOwnProfile(UUID id, String username, String phone) {
        var account = account(id);
        var identity = AdminIdentityNormalizer.normalize(username, account.email());
        validateProfile(identity.username(), identity.email(), phone);
        String normalizedPhone = phone == null || phone.isBlank() ? null : phone.strip();
        try {
            return repository.updateOwnProfile(id, identity.username(), normalizedPhone);
        } catch (DuplicateKeyException exception) {
            throw adminExists();
        }
    }

    /** 按邮箱查询 active 状态的管理账号（供忘记密码等场景使用）。 */
    @Transactional(readOnly = true)
    public Optional<AdminAccount> activeAccountByEmail(String email) {
        String normalized = validatedEmail(email);
        return repository.findActiveByEmail(normalized);
    }

    /** 校验并归一化新邮箱（与当前邮箱去重、避免被其他账号占用），返回归一化结果。 */
    @Transactional(readOnly = true)
    public String prepareOwnEmailChange(UUID id, String email) {
        var account = account(id);
        String normalized = validatedEmail(email);
        if (account.email().equalsIgnoreCase(normalized)) {
            throw new DomainException(ErrorCodeConstants.EMAIL_UNCHANGED);
        }
        if (repository.emailExistsForOtherAccount(id, normalized)) {
            throw adminExists();
        }
        return normalized;
    }

    /** 修改当前账号邮箱并吊销全部会话（使旧令牌失效）。 */
    @Transactional
    public AdminAccount changeOwnEmail(UUID id, String email) {
        String normalized = prepareOwnEmailChange(id, email);
        try {
            return repository.changeOwnEmail(id, normalized);
        } catch (DuplicateKeyException exception) {
            throw adminExists();
        }
    }

    /** 更新账号状态（active/disabled），禁止操作自己，变更后吊销其全部会话。 */
    @Transactional
    public AdminAccount updateStatus(UUID actor, UUID id, String status) {
        if (actor.equals(id)) {
            throw new DomainException(ErrorCodeConstants.SELF_STATUS_CHANGE);
        }
        if (!java.util.Set.of("active", "disabled").contains(status)) {
            throw new DomainException(ErrorCodeConstants.STATUS_INVALID);
        }
        var account = repository.updateStatus(id, status);
        repository.revokeAll(id);
        return account;
    }

    /** 重置指定账号密码（禁止操作自己），变更后吊销其全部会话。 */
    @Transactional
    public void resetPassword(UUID actor, UUID id, String password) {
        assertNotActor(actor, id, ErrorCodeConstants.SELF_PASSWORD_RESET_DENIED);
        validatePassword(password);
        repository.resetPassword(id, passwords.encode(password));
        repository.revokeAll(id);
    }

    /** 修改自己的密码：校验当前密码与安全策略，并记录历史密码防重用。 */
    @Transactional
    public void changePassword(UUID id, String current, String replacement) {
        var account = repository.findActive(id).orElseThrow(AdminIdentityService::invalidCredentials);
        var credential = repository.findCredential(account.username()).orElseThrow(AdminIdentityService::invalidCredentials);
        if (!passwords.matches(current, credential.passwordHash())) {
            throw new DomainException(ErrorCodeConstants.CURRENT_PASSWORD_INVALID);
        }
        security.validatePassword(replacement, SecurityPolicyService.adminPolicy());
        security.rejectReusedPassword("admin", id, replacement, credential.passwordHash(), passwords, SecurityPolicyService.adminPolicy().historyCount());
        repository.resetPassword(id, passwords.encode(replacement));
        security.rememberPassword("admin", id, credential.passwordHash(), SecurityPolicyService.adminPolicy().historyCount());
        repository.revokeAll(id);
    }

    /** 重置指定账号的 MFA（禁止操作自己），变更后吊销其全部会话。 */
    @Transactional
    public AdminAccount resetMfa(UUID actor, UUID id) {
        assertNotActor(actor, id, ErrorCodeConstants.SELF_MFA_RESET_DENIED);
        var account = repository.resetMfa(id);
        repository.revokeAll(id);
        return account;
    }

    /** 关闭当前账号的 MFA 并吊销其全部会话。 */
    @Transactional
    public AdminAccount disableMfa(UUID id) {
        var account = repository.resetMfa(id);
        repository.revokeAll(id);
        return account;
    }

    /** 为当前账号开启指定类型的 MFA 并吊销其全部会话。 */
    @Transactional
    public AdminAccount enableMfa(UUID id, String type) {
        var account = repository.enableMfa(id, type);
        repository.revokeAll(id);
        return account;
    }

    /** 查询 active 状态的管理账号（不存在时抛凭证无效异常）。 */
    @Transactional(readOnly = true)
    public AdminAccount account(UUID id) {
        return repository.findActive(id).orElseThrow(AdminIdentityService::invalidCredentials);
    }

    /** 实现 {@link ActiveAdminAccountLocker}：加锁校验账号存在且为 active 状态。 */
    @Override
    @Transactional
    public void lockActive(UUID accountId) {
        if (accountId == null) {
            throw new DomainException(ErrorCodeConstants.ADMINISTRATOR_ACCOUNT_REQUIRED);
        }
        repository.lockActive(accountId).orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMINISTRATOR_ACCOUNT_NOT_ACTIVE));
    }

    /** MFA 校验通过后完成登录：记录登录时间并签发刷新令牌。 */
    @Transactional
    public AuthenticatedAdmin completeMfa(UUID id, Duration ttl, String userAgent, String ip) {
        var account = account(id);
        repository.recordLogin(id);
        return new AuthenticatedAdmin(account, issueRefresh(account, ttl, userAgent, ip).token());
    }

    /** 生成 32 字节随机刷新令牌并创建会话（仅落库 SHA-256 哈希），返回会话 ID 与明文令牌。 */
    private IssuedRefresh issueRefresh(AdminAccount account, Duration ttl, String agent, String ip) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UUID id = UuidV7.randomUuid();
        repository.createSession(id, account.id(), TokenHash.sha256(token), account.securityVersion(), Instant.now().plus(ttl), trim(agent, 512), trim(ip, 64));
        return new IssuedRefresh(id, token);
    }

    /** 创建账号（编码密码，处理唯一键冲突）。 */
    private AdminAccount createAccount(AdminIdentityNormalizer.NormalizedIdentity identity, String password) {
        try {
            return repository.create(identity.username(), identity.email(), passwords.encode(password));
        } catch (DuplicateKeyException exception) {
            throw adminExists();
        }
    }

    /** 校验用户名、邮箱与密码的合法性。 */
    private static void validateCredentials(String username, String email, String password) {
        validateProfile(username, email, null);
        validatePassword(password);
    }

    /** 校验用户名/邮箱/手机号的格式与长度限制。 */
    private static void validateProfile(String username, String email, String phone) {
        if (username == null || username.isBlank() || username.length() > 100) {
            throw new DomainException(ErrorCodeConstants.USERNAME_INVALID);
        }
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 320) {
            throw new DomainException(ErrorCodeConstants.EMAIL_INVALID);
        }
        if (phone != null && !phone.isBlank() && (phone.strip().length() > 32 || !phone.strip().matches("^\\+?[0-9][0-9 -]{5,31}$"))) {
            throw new DomainException(ErrorCodeConstants.PHONE_INVALID);
        }
    }

    /** 归一化并校验邮箱格式，返回归一化结果。 */
    private static String validatedEmail(String email) {
        String normalized = AdminIdentityNormalizer.normalizeEmail(email);
        if (normalized == null || !normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || normalized.length() > 320) {
            throw new DomainException(ErrorCodeConstants.EMAIL_INVALID);
        }
        return normalized;
    }

    /** 校验密码强度：8-128 位且同时包含大小写字母和数字。 */
    private static void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128 || !password.matches(".*[a-z].*") || !password.matches(".*[A-Z].*") || !password.matches(".*\\d.*")) {
            throw new DomainException(ErrorCodeConstants.PASSWORD_WEAK);
        }
    }

    /** 禁止对自身账号执行敏感操作（如重置密码、重置 MFA、修改状态）。 */
    private static void assertNotActor(UUID actor, UUID account, com.easy1auth.foundation.error.ErrorCode errorCode) {
        if (actor != null && actor.equals(account)) {
            throw new DomainException(errorCode);
        }
    }

    /** 构造“用户名或邮箱已被注册”异常。 */
    private static DomainException adminExists() {
        return new DomainException(ErrorCodeConstants.ADMIN_EXISTS_USERNAME_OR_EMAIL);
    }

    /**
     * A failed interactive login is a business validation failure, not an expired
     * authentication session.  Reserve HTTP 401 for invalid bearer/refresh tokens
     * so clients do not clear their local session and redirect while handling a
     * bad password.
     */
    private static DomainException invalidCredentials() {
        return new DomainException(ErrorCodeConstants.INVALID_CREDENTIALS);
    }

    /** 构造“刷新令牌无效或已失效”异常。 */
    private static DomainException invalidRefresh() {
        return new DomainException(ErrorCodeConstants.INVALID_REFRESH_TOKEN);
    }

    /** 截断字符串至最大长度（控制 UA/IP 落库长度）。 */
    private static String trim(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(value.length(), max));
    }

    /**
     * 刷新令牌签发结果（内部使用）。
     *
     * @param id    新会话 ID
     * @param token 明文刷新令牌（仅本次返回）
     */
    private record IssuedRefresh(UUID id, String token) {
    }
}
