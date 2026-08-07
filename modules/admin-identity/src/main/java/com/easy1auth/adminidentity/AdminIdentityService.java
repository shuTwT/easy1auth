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

@Service
public class AdminIdentityService implements ActiveAdminAccountLocker {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AdminIdentityRepository repository;
    private final PasswordEncoder passwords;
    private final LoginProtectionService protection;
    private final SecurityPolicyService security;

    AdminIdentityService(AdminIdentityRepository repository, PasswordEncoder passwords, LoginProtectionService protection, SecurityPolicyService security) {
        this.repository = repository;
        this.passwords = passwords;
        this.protection = protection;
        this.security = security;
    }

    @Transactional
    public AuthenticatedAdmin authenticate(String login, String password, Duration refreshTtl, String userAgent, String ip) {
        if (login == null || password == null) throw invalidCredentials();
        String key = AdminIdentityNormalizer.normalizeLogin(login);
        protection.assertAllowed("admin", key, null);
        var found = repository.findCredential(key);
        if (found.isEmpty()) {
            protection.failed("admin", key, null, 5, 1800);
            throw invalidCredentials();
        }
        var row = found.get();
        if (!"active".equals(row.account().status())) throw new DomainException(ErrorCodeConstants.ADMIN_DISABLED);
        if (!passwords.matches(password, row.passwordHash())) {
            protection.failed("admin", key, null, 5, 1800);
            throw invalidCredentials();
        }
        protection.succeeded("admin", key, null);
        repository.recordLogin(row.account().id());
        return new AuthenticatedAdmin(row.account(), issueRefresh(row.account(), refreshTtl, userAgent, ip).token());
    }

    @Transactional
    public AuthenticatedAdmin register(String username, String email, String password, String code, Duration refreshTtl, String userAgent, String ip) {
        var identity = AdminIdentityNormalizer.normalize(username, email);
        validateCredentials(identity.username(), identity.email(), password);
        if (code == null || code.isBlank() || !repository.consumeRegistrationCode(identity.email(), TokenHash.sha256(code)))
            throw new DomainException(ErrorCodeConstants.VERIFICATION_CODE_INVALID);
        if (repository.exists(identity.username(), identity.email())) throw adminExists();
        var account = createAccount(identity, password);
        return new AuthenticatedAdmin(account, issueRefresh(account, refreshTtl, userAgent, ip).token());
    }

    @Transactional
    public AdminAccount createAdministrator(String username, String email, String password) {
        var identity = AdminIdentityNormalizer.normalize(username, email);
        validateCredentials(identity.username(), identity.email(), password);
        if (repository.exists(identity.username(), identity.email())) throw adminExists();
        return createAccount(identity, password);
    }

    @Transactional(readOnly = true)
    public AdminAccount validateTokenSubject(UUID accountId, long securityVersion) {
        var account = repository.findActive(accountId).orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_SESSION_INVALID));
        if (account.securityVersion() != securityVersion)
            throw new DomainException(ErrorCodeConstants.ADMIN_SESSION_INVALID);
        return account;
    }

    @Transactional
    public RefreshSession rotate(String token, Duration ttl, String userAgent, String ip) {
        if (token == null || token.isBlank()) throw invalidRefresh();
        var old = repository.lockSession(TokenHash.sha256(token)).orElseThrow(AdminIdentityService::invalidRefresh);
        var account = old.account();
        if (!"active".equals(account.status()) || account.securityVersion() != old.sessionSecurityVersion())
            throw invalidRefresh();
        var replacement = issueRefresh(account, ttl, userAgent, ip);
        repository.rotate(old.id(), replacement.id());
        return new RefreshSession(old.id(), account, replacement.token());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) repository.revoke(TokenHash.sha256(refreshToken));
    }

    @Transactional
    public void logoutAll(UUID accountId) {
        repository.revokeAll(accountId);
    }

    @Transactional
    public void logoutAllAndInvalidate(UUID accountId) {
        repository.invalidateAccountSessions(accountId);
    }

    @Transactional
    public AdminAccount updateProfile(UUID id, String username, String email, String phone) {
        return repository.updateProfile(id, username, email, phone);
    }

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

    @Transactional(readOnly = true)
    public Optional<AdminAccount> activeAccountByEmail(String email) {
        String normalized = validatedEmail(email);
        return repository.findActiveByEmail(normalized);
    }

    @Transactional(readOnly = true)
    public String prepareOwnEmailChange(UUID id, String email) {
        var account = account(id);
        String normalized = validatedEmail(email);
        if (account.email().equalsIgnoreCase(normalized))
            throw new DomainException(ErrorCodeConstants.EMAIL_UNCHANGED);
        if (repository.emailExistsForOtherAccount(id, normalized)) throw adminExists();
        return normalized;
    }

    @Transactional
    public AdminAccount changeOwnEmail(UUID id, String email) {
        String normalized = prepareOwnEmailChange(id, email);
        try {
            return repository.changeOwnEmail(id, normalized);
        } catch (DuplicateKeyException exception) {
            throw adminExists();
        }
    }

    @Transactional
    public AdminAccount updateStatus(UUID actor, UUID id, String status) {
        if (actor.equals(id)) throw new DomainException(ErrorCodeConstants.SELF_STATUS_CHANGE);
        if (!java.util.Set.of("active", "disabled").contains(status))
            throw new DomainException(ErrorCodeConstants.STATUS_INVALID);
        var account = repository.updateStatus(id, status);
        repository.revokeAll(id);
        return account;
    }

    @Transactional
    public void resetPassword(UUID actor, UUID id, String password) {
        assertNotActor(actor, id, ErrorCodeConstants.SELF_PASSWORD_RESET_DENIED);
        validatePassword(password);
        repository.resetPassword(id, passwords.encode(password));
        repository.revokeAll(id);
    }

    @Transactional
    public void changePassword(UUID id, String current, String replacement) {
        var account = repository.findActive(id).orElseThrow(AdminIdentityService::invalidCredentials);
        var credential = repository.findCredential(account.username()).orElseThrow(AdminIdentityService::invalidCredentials);
        if (!passwords.matches(current, credential.passwordHash()))
            throw new DomainException(ErrorCodeConstants.CURRENT_PASSWORD_INVALID);
        security.validatePassword(replacement, SecurityPolicyService.adminPolicy());
        security.rejectReusedPassword("admin", id, replacement, credential.passwordHash(), passwords, SecurityPolicyService.adminPolicy().historyCount());
        repository.resetPassword(id, passwords.encode(replacement));
        security.rememberPassword("admin", id, credential.passwordHash(), SecurityPolicyService.adminPolicy().historyCount());
        repository.revokeAll(id);
    }

    @Transactional
    public AdminAccount resetMfa(UUID actor, UUID id) {
        assertNotActor(actor, id, ErrorCodeConstants.SELF_MFA_RESET_DENIED);
        var account = repository.resetMfa(id);
        repository.revokeAll(id);
        return account;
    }

    @Transactional
    public AdminAccount disableMfa(UUID id) {
        var account = repository.resetMfa(id);
        repository.revokeAll(id);
        return account;
    }

    @Transactional
    public AdminAccount enableMfa(UUID id, String type) {
        var account = repository.enableMfa(id, type);
        repository.revokeAll(id);
        return account;
    }

    @Transactional(readOnly = true)
    public AdminAccount account(UUID id) {
        return repository.findActive(id).orElseThrow(AdminIdentityService::invalidCredentials);
    }

    @Override
    @Transactional
    public void lockActive(UUID accountId) {
        if (accountId == null) {
            throw new DomainException(ErrorCodeConstants.ADMINISTRATOR_ACCOUNT_REQUIRED);
        }
        repository.lockActive(accountId).orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMINISTRATOR_ACCOUNT_NOT_ACTIVE));
    }

    @Transactional
    public AuthenticatedAdmin completeMfa(UUID id, Duration ttl, String userAgent, String ip) {
        var account = account(id);
        repository.recordLogin(id);
        return new AuthenticatedAdmin(account, issueRefresh(account, ttl, userAgent, ip).token());
    }

    private IssuedRefresh issueRefresh(AdminAccount account, Duration ttl, String agent, String ip) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UUID id = UuidV7.randomUuid();
        repository.createSession(id, account.id(), TokenHash.sha256(token), account.securityVersion(), Instant.now().plus(ttl), trim(agent, 512), trim(ip, 64));
        return new IssuedRefresh(id, token);
    }

    private AdminAccount createAccount(AdminIdentityNormalizer.NormalizedIdentity identity, String password) {
        try {
            return repository.create(identity.username(), identity.email(), passwords.encode(password));
        } catch (DuplicateKeyException exception) {
            throw adminExists();
        }
    }

    private static void validateCredentials(String username, String email, String password) {
        validateProfile(username, email, null);
        validatePassword(password);
    }

    private static void validateProfile(String username, String email, String phone) {
        if (username == null || username.isBlank() || username.length() > 100)
            throw new DomainException(ErrorCodeConstants.USERNAME_INVALID);
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 320)
            throw new DomainException(ErrorCodeConstants.EMAIL_INVALID);
        if (phone != null && !phone.isBlank() && (phone.strip().length() > 32 || !phone.strip().matches("^\\+?[0-9][0-9 -]{5,31}$")))
            throw new DomainException(ErrorCodeConstants.PHONE_INVALID);
    }

    private static String validatedEmail(String email) {
        String normalized = AdminIdentityNormalizer.normalizeEmail(email);
        if (normalized == null || !normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || normalized.length() > 320)
            throw new DomainException(ErrorCodeConstants.EMAIL_INVALID);
        return normalized;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128 || !password.matches(".*[a-z].*") || !password.matches(".*[A-Z].*") || !password.matches(".*\\d.*"))
            throw new DomainException(ErrorCodeConstants.PASSWORD_WEAK);
    }

    private static void assertNotActor(UUID actor, UUID account, com.easy1auth.foundation.error.ErrorCode errorCode) {
        if (actor != null && actor.equals(account)) throw new DomainException(errorCode);
    }

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

    private static DomainException invalidRefresh() {
        return new DomainException(ErrorCodeConstants.INVALID_REFRESH_TOKEN);
    }

    private static String trim(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(value.length(), max));
    }

    private record IssuedRefresh(UUID id, String token) {
    }
}
