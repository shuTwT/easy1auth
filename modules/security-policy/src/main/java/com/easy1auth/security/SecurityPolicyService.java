package com.easy1auth.security;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.security.model.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class SecurityPolicyService {
    private static final AuthenticationRecoveryCodeEntityTable RECOVERY = AuthenticationRecoveryCodeEntityTable.$;
    private static final PasswordHistoryEntityTable HISTORY = PasswordHistoryEntityTable.$;
    private static final SecurityPolicyEntityTable POLICY = SecurityPolicyEntityTable.$;
    private static final AuthenticationFactorEntityTable FACTOR = AuthenticationFactorEntityTable.$;
    private static final AuthenticationChallengeEntityTable CHALLENGE = AuthenticationChallengeEntityTable.$;
    private final JSqlClient sql;
    private final SecurityDataCipher cipher;
    private final TotpService totp;
    private final SecureRandom random = new SecureRandom();

    public SecurityPolicyService(JSqlClient sql, SecurityDataCipher cipher, TotpService totp) {
        this.sql = sql;
        this.cipher = cipher;
        this.totp = totp;
    }

    public static Policy adminPolicy() {
        return new Policy(12, true, true, true, true, 90, 5, false, 5, 1800);
    }

    @Transactional
    public Policy policy(UUID tenant) {
        return view(policyEntity(tenant));
    }

    @Transactional
    public Policy update(UUID tenant, Policy p) {
        validate(p);
        var old = policyEntity(tenant);
        var e = SecurityPolicyEntityDraft.$.produce(d -> d.setId(old.id()).setTenantId(tenant).setPasswordMinLength(p.minLength()).setPasswordRequireUpper(p.requireUpper()).setPasswordRequireLower(p.requireLower()).setPasswordRequireNumber(p.requireNumber()).setPasswordRequireSpecial(p.requireSpecial()).setPasswordMaxAgeDays(p.maxAgeDays()).setPasswordHistoryCount(p.historyCount()).setMfaRequired(p.mfaRequired()).setLoginAttemptLimit(p.loginAttemptLimit()).setLockoutDurationSeconds(p.lockoutSeconds()).setUpdatedAt(Instant.now()));
        sql.saveCommand(e).setMode(SaveMode.UPSERT).execute();
        return p;
    }

    public void validatePassword(String password, Policy p) {
        if (password == null || password.length() < p.minLength() || password.length() > 128 || (p.requireUpper() && !password.matches(".*[A-Z].*")) || (p.requireLower() && !password.matches(".*[a-z].*")) || (p.requireNumber() && !password.matches(".*\\d.*")) || (p.requireSpecial() && !password.matches(".*[^A-Za-z0-9].*")))
            throw new DomainException("PASSWORD_WEAK", "密码不符合安全策略", 400);
    }

    @Transactional(readOnly = true)
    public void rejectReusedPassword(String subjectType, UUID subject, String candidate, String currentHash, org.springframework.security.crypto.password.PasswordEncoder encoder, int count) {
        if (currentHash != null && encoder.matches(candidate, currentHash))
            throw new DomainException("PASSWORD_REUSED", "不能重复使用当前密码", 400);
        if (count <= 0) return;
        var hashes = sql.createQuery(HISTORY).where(HISTORY.subjectType().eq(subjectType), HISTORY.subjectId().eq(subject)).orderBy(HISTORY.createdAt().desc()).select(HISTORY.passwordHash()).limit(count).execute();
        if (hashes.stream().anyMatch(h -> encoder.matches(candidate, h)))
            throw new DomainException("PASSWORD_REUSED", "不能使用最近使用过的密码", 400);
    }

    @Transactional
    public void rememberPassword(String subjectType, UUID subject, String oldHash, int count) {
        if (oldHash == null || count <= 0) return;
        var e = PasswordHistoryEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setSubjectType(subjectType).setSubjectId(subject).setPasswordHash(oldHash).setCreatedAt(Instant.now()));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        var ids = sql.createQuery(HISTORY).where(HISTORY.subjectType().eq(subjectType), HISTORY.subjectId().eq(subject)).orderBy(HISTORY.createdAt().desc()).select(HISTORY.id()).execute();
        if (ids.size() > count)
            sql.createDelete(HISTORY).where(HISTORY.id().in(ids.subList(count, ids.size()))).execute();
    }

    @Transactional
    public Setup setupTotp(String subjectType, UUID subject, UUID tenant, String label) {
        var old = findFactor(subjectType, subject, "totp");
        if (old != null && old.enabled()) throw new DomainException("MFA_ALREADY_ENABLED", "TOTP 已启用", 409);
        String secret = totp.secret();
        UUID id = old == null ? UuidV7.randomUuid() : old.id();
        Instant now = Instant.now();
        var entity = AuthenticationFactorEntityDraft.$.produce(d -> d.setId(id).setSubjectType(subjectType).setSubjectId(subject).setTenantId(tenant).setFactorType("totp").setEncryptedSecret(cipher.encrypt(aad(subjectType, subject, "totp"), secret)).setEnabled(false).setLastTotpStep(null).setCreatedAt(old == null ? now : old.createdAt()).setUpdatedAt(now));
        sql.saveCommand(entity).setMode(SaveMode.UPSERT).execute();
        sql.createDelete(RECOVERY).where(RECOVERY.factorId().eq(id)).execute();
        List<String> recovery = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String code = randomToken(8);
            recovery.add(code);
            var saved = AuthenticationRecoveryCodeEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setFactorId(id).setCodeHash(hash(code)).setUsedAt(null).setCreatedAt(now));
            sql.saveCommand(saved).setMode(SaveMode.INSERT_ONLY).execute();
        }
        return new Setup(secret, "otpauth://totp/Easy1Auth:" + url(label) + "?secret=" + secret + "&issuer=Easy1Auth&digits=6&period=30", recovery);
    }

    @Transactional
    public void enableTotp(String subjectType, UUID subject, String code) {
        var f = requireFactor(subjectType, subject, "totp");
        String secret = cipher.decrypt(aad(subjectType, subject, "totp"), f.encryptedSecret());
        if (!totp.verify(secret, code, Instant.now(), f.lastTotpStep()))
            throw new DomainException("MFA_CODE_INVALID", "验证码无效", 400);
        sql.createUpdate(FACTOR).set(FACTOR.enabled(), true).set(FACTOR.lastTotpStep(), totp.step(Instant.now())).set(FACTOR.updatedAt(), Instant.now()).where(FACTOR.id().eq(f.id())).execute();
    }

    @Transactional
    public void disable(String subjectType, UUID subject, String code) {
        verifyTotp(subjectType, subject, code);
        sql.createDelete(FACTOR).where(FACTOR.subjectType().eq(subjectType), FACTOR.subjectId().eq(subject)).execute();
    }

    @Transactional
    public boolean verifyTotp(String subjectType, UUID subject, String code) {
        var f = requireFactor(subjectType, subject, "totp");
        if (!f.enabled()) throw new DomainException("MFA_NOT_ENABLED", "MFA 未启用", 409);
        String secret = cipher.decrypt(aad(subjectType, subject, "totp"), f.encryptedSecret());
        Instant now = Instant.now();
        if (!totp.verify(secret, code, now, f.lastTotpStep()))
            throw new DomainException("MFA_CODE_INVALID", "验证码无效", 400);
        sql.createUpdate(FACTOR).set(FACTOR.lastTotpStep(), totp.step(now)).set(FACTOR.updatedAt(), now).where(FACTOR.id().eq(f.id()), Predicate.or(FACTOR.lastTotpStep().isNull(), FACTOR.lastTotpStep().lt(totp.step(now)))).execute();
        return true;
    }

    @Transactional
    public Challenge issueEmailChallenge(String subjectType, UUID subject, UUID tenant, String purpose) {
        Instant now = Instant.now();
        var recent = sql.createQuery(CHALLENGE).where(CHALLENGE.subjectType().eq(subjectType), CHALLENGE.subjectId().eq(subject), CHALLENGE.factorType().eq("email"), CHALLENGE.createdAt().gt(now.minusSeconds(60))).select(CHALLENGE.id()).exists();
        if (recent) throw new DomainException("CODE_RATE_LIMITED", "验证码发送过于频繁", 429);
        String token = randomToken(32), code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        var e = AuthenticationChallengeEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTokenHash(hash(token)).setSubjectType(subjectType).setSubjectId(subject).setTenantId(tenant).setPurpose(purpose).setFactorType("email").setCodeHash(hash(code)).setAttempts(0).setMaxAttempts(5).setExpiresAt(now.plusSeconds(600)).setConsumedAt(null).setCreatedAt(now).setLastSentAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return new Challenge(token, code, 600);
    }

    @Transactional
    public UUID consumeEmailChallenge(String token, String code) {
        var row = sql.createQuery(CHALLENGE).where(CHALLENGE.tokenHash().eq(hash(token)), CHALLENGE.factorType().eq("email")).select(CHALLENGE).forUpdate().fetchOneOrNull();
        if (row == null || row.subjectId() == null || row.consumedAt() != null || row.expiresAt().isBefore(Instant.now()) || row.attempts() >= row.maxAttempts())
            throw new DomainException("MFA_CHALLENGE_INVALID", "MFA 挑战无效或已过期", 401);
        if (!MessageDigest.isEqual(hash(code).getBytes(StandardCharsets.US_ASCII), row.codeHash().getBytes(StandardCharsets.US_ASCII))) {
            sql.createUpdate(CHALLENGE).set(CHALLENGE.attempts(), CHALLENGE.attempts().plus(1)).where(CHALLENGE.id().eq(row.id())).execute();
            throw new DomainException("MFA_CODE_INVALID", "验证码无效", 400);
        }
        if (sql.createUpdate(CHALLENGE).set(CHALLENGE.consumedAt(), Instant.now()).where(CHALLENGE.id().eq(row.id()), CHALLENGE.consumedAt().isNull()).execute() != 1)
            throw new DomainException("MFA_CHALLENGE_REPLAYED", "MFA 挑战已使用", 401);
        return row.subjectId();
    }

    @Transactional
    public Challenge issueTotpChallenge(String subjectType, UUID subject, UUID tenant, String purpose) {
        var factor = requireFactor(subjectType, subject, "totp");
        if (!factor.enabled()) throw new DomainException("MFA_NOT_ENABLED", "MFA 未启用", 409);
        String token = randomToken(32);
        Instant now = Instant.now();
        var e = AuthenticationChallengeEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTokenHash(hash(token)).setSubjectType(subjectType).setSubjectId(subject).setTenantId(tenant).setPurpose(purpose).setFactorType("totp").setCodeHash(null).setAttempts(0).setMaxAttempts(5).setExpiresAt(now.plusSeconds(600)).setConsumedAt(null).setCreatedAt(now).setLastSentAt(null));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return new Challenge(token, null, 600);
    }

    @Transactional
    public UUID consumeTotpChallenge(String token, String code) {
        var row = sql.createQuery(CHALLENGE).where(CHALLENGE.tokenHash().eq(hash(token)), CHALLENGE.factorType().eq("totp")).select(CHALLENGE).forUpdate().fetchOneOrNull();
        if (row == null || row.consumedAt() != null || row.expiresAt().isBefore(Instant.now()) || row.attempts() >= row.maxAttempts() || row.subjectId() == null)
            throw new DomainException("MFA_CHALLENGE_INVALID", "MFA 挑战无效或已过期", 401);
        try {
            verifyTotp(row.subjectType(), row.subjectId(), code);
        } catch (DomainException ex) {
            sql.createUpdate(CHALLENGE).set(CHALLENGE.attempts(), CHALLENGE.attempts().plus(1)).where(CHALLENGE.id().eq(row.id())).execute();
            throw ex;
        }
        if (sql.createUpdate(CHALLENGE).set(CHALLENGE.consumedAt(), Instant.now()).where(CHALLENGE.id().eq(row.id()), CHALLENGE.consumedAt().isNull()).execute() != 1)
            throw new DomainException("MFA_CHALLENGE_REPLAYED", "MFA 挑战已使用", 401);
        return row.subjectId();
    }

    @Transactional(readOnly = true)
    public Status status(String type, UUID subject) {
        var rows = sql.createQuery(FACTOR).where(FACTOR.subjectType().eq(type), FACTOR.subjectId().eq(subject), FACTOR.enabled().eq(true)).select(FACTOR.factorType()).execute();
        return new Status(!rows.isEmpty(), rows);
    }

    private AuthenticationFactorEntity findFactor(String type, UUID subject, String factor) {
        return sql.createQuery(FACTOR).where(FACTOR.subjectType().eq(type), FACTOR.subjectId().eq(subject), FACTOR.factorType().eq(factor)).select(FACTOR).fetchOneOrNull();
    }

    private AuthenticationFactorEntity requireFactor(String type, UUID subject, String factor) {
        var f = findFactor(type, subject, factor);
        if (f == null) throw new DomainException("MFA_NOT_CONFIGURED", "MFA 尚未配置", 404);
        return f;
    }

    private SecurityPolicyEntity policyEntity(UUID tenant) {
        var found = sql.createQuery(POLICY).where(POLICY.tenantId().eq(tenant)).select(POLICY).fetchOneOrNull();
        if (found == null) {
            found = SecurityPolicyEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setPasswordMinLength(8).setPasswordRequireUpper(true).setPasswordRequireLower(true).setPasswordRequireNumber(true).setPasswordRequireSpecial(true).setPasswordMaxAgeDays(90).setPasswordHistoryCount(5).setMfaRequired(false).setLoginAttemptLimit(5).setLockoutDurationSeconds(1800).setUpdatedAt(Instant.now()));
            sql.saveCommand(found).setMode(SaveMode.INSERT_IF_ABSENT).execute();
        }
        return found;
    }

    private static Policy view(SecurityPolicyEntity e) {
        return new Policy(e.passwordMinLength(), e.passwordRequireUpper(), e.passwordRequireLower(), e.passwordRequireNumber(), e.passwordRequireSpecial(), e.passwordMaxAgeDays(), e.passwordHistoryCount(), e.mfaRequired(), e.loginAttemptLimit(), e.lockoutDurationSeconds());
    }

    private static void validate(Policy p) {
        if (p == null || p.minLength() < 8 || p.minLength() > 128 || p.historyCount() < 0 || p.historyCount() > 24 || p.loginAttemptLimit() < 1 || p.lockoutSeconds() < 60)
            throw new DomainException("SECURITY_POLICY_INVALID", "安全策略参数无效", 400);
    }

    private static String aad(String type, UUID id, String factor) {
        return type + ":" + id + ":" + factor;
    }

    private static String url(String v) {
        return java.net.URLEncoder.encode(v == null ? "user" : v, StandardCharsets.UTF_8);
    }

    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        random.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Objects.toString(value, "").getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record Policy(int minLength, boolean requireUpper, boolean requireLower, boolean requireNumber,
                         boolean requireSpecial, int maxAgeDays, int historyCount, boolean mfaRequired,
                         int loginAttemptLimit, int lockoutSeconds) {
    }

    public record Setup(String secret, String qrCodeUrl, List<String> backupCodes) {
    }

    public record Challenge(String token, String code, int expiresIn) {
    }

    public record Status(boolean enabled, List<String> methods) {
    }
}
