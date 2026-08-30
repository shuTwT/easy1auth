package com.easy1auth.security.service;

import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.id.UuidV7;
import com.easy1auth.security.ErrorCodeConstants;
import com.easy1auth.security.SecurityDataCipher;
import com.easy1auth.security.dto.*;
import com.easy1auth.security.dto.PolicyView;
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

/**
 * 安全策略服务：密码策略、TOTP 多因素认证与邮箱/验证码挑战的统一入口。
 *
 * <p>负责密码强度校验与历史密码防重用、TOTP 密钥的生成/启用/验证/关闭，
 * 以及邮箱验证码与 TOTP 两类认证挑战（challenge）的签发与消费。所有
 * 敏感材料（TOTP 密钥、验证码）均以哈希或加密形式落库。</p>
 */
@Service
public class SecurityPolicyService {
    /** authentication_recovery_code 表静态描述符 */
    private static final AuthenticationRecoveryCodeEntityTable RECOVERY = AuthenticationRecoveryCodeEntityTable.$;
    /** password_history 表静态描述符 */
    private static final PasswordHistoryEntityTable HISTORY = PasswordHistoryEntityTable.$;
    /** security_policy 表静态描述符 */
    private static final SecurityPolicyEntityTable POLICY = SecurityPolicyEntityTable.$;
    /** authentication_factor 表静态描述符 */
    private static final AuthenticationFactorEntityTable FACTOR = AuthenticationFactorEntityTable.$;
    /** authentication_challenge 表静态描述符 */
    private static final AuthenticationChallengeEntityTable CHALLENGE = AuthenticationChallengeEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;
    /** 数据加密器（加密 TOTP 密钥） */
    private final SecurityDataCipher cipher;
    /** TOTP 计算服务 */
    private final TotpService totp;
    /** 安全随机源（生成挑战 token 与备用码） */
    private final SecureRandom random = new SecureRandom();

    public SecurityPolicyService(JSqlClient sql, SecurityDataCipher cipher, TotpService totp) {
        this.sql = sql;
        this.cipher = cipher;
        this.totp = totp;
    }

    /** 平台管理员的默认安全策略（不落库，直接返回）。 */
    public static PolicyView adminPolicy() {
        return new PolicyView(12, true, true, true, true, 90, 5, false, 5, 1800);
    }

    /** 读取全局（平台）安全策略，不存在时按默认值初始化。 */
    @Transactional
    public PolicyView policy() {
        return view(policyEntity());
    }

    /** 读取指定租户的安全策略，不存在时按默认值初始化。 */
    @Transactional
    public PolicyView policy(UUID tenant) {
        return view(policyEntity(tenant));
    }

    /** 更新安全策略：先校验取值范围，再整体覆盖保存。 */
    @Transactional
    public PolicyView update(PolicyView p) {
        validate(p);
        var old = policyEntity();
        var e = SecurityPolicyEntityDraft.$.produce(d -> d.setId(old.id()).setPasswordMinLength(p.minLength()).setPasswordRequireUpper(p.requireUpper()).setPasswordRequireLower(p.requireLower()).setPasswordRequireNumber(p.requireNumber()).setPasswordRequireSpecial(p.requireSpecial()).setPasswordMaxAgeDays(p.maxAgeDays()).setPasswordHistoryCount(p.historyCount()).setMfaRequired(p.mfaRequired()).setLoginAttemptLimit(p.loginAttemptLimit()).setLockoutDurationSeconds(p.lockoutSeconds()).setUpdatedAt(Instant.now()));
        sql.saveCommand(e).setMode(SaveMode.UPSERT).execute();
        return p;
    }

    /** 校验密码是否符合策略：长度 8-128，并按需要求大小写字母、数字与特殊字符。 */
    public void validatePassword(String password, PolicyView p) {
        if (password == null || password.length() < p.minLength() || password.length() > 128 || (p.requireUpper() && !password.matches(".*[A-Z].*")) || (p.requireLower() && !password.matches(".*[a-z].*")) || (p.requireNumber() && !password.matches(".*\\d.*")) || (p.requireSpecial() && !password.matches(".*[^A-Za-z0-9].*"))) {
            throw new DomainException(ErrorCodeConstants.PASSWORD_WEAK);
        }
    }

    /** 拒绝与当前密码或最近 {@code count} 次历史密码相同的候选密码。 */
    @Transactional(readOnly = true)
    public void rejectReusedPassword(String subjectType, UUID subject, String candidate, String currentHash, org.springframework.security.crypto.password.PasswordEncoder encoder, int count) {
        if (currentHash != null && encoder.matches(candidate, currentHash)) {
            throw new DomainException(ErrorCodeConstants.PASSWORD_REUSED_CURRENT);
        }
        if (count <= 0) {
            return;
        }
        var hashes = sql.createQuery(HISTORY).where(HISTORY.subjectType().eq(subjectType), HISTORY.subjectId().eq(subject)).orderBy(HISTORY.createdAt().desc()).select(HISTORY.passwordHash()).limit(count).execute();
        if (hashes.stream().anyMatch(h -> encoder.matches(candidate, h))) {
            throw new DomainException(ErrorCodeConstants.PASSWORD_REUSED_RECENT);
        }
    }

    /** 记录一次密码变更：保存旧密码哈希，并裁剪历史记录只保留最近 {@code count} 条。 */
    @Transactional
    public void rememberPassword(String subjectType, UUID subject, String oldHash, int count) {
        if (oldHash == null || count <= 0) {
            return;
        }
        var e = PasswordHistoryEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setSubjectType(subjectType).setSubjectId(subject).setPasswordHash(oldHash).setCreatedAt(Instant.now()));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        var ids = sql.createQuery(HISTORY).where(HISTORY.subjectType().eq(subjectType), HISTORY.subjectId().eq(subject)).orderBy(HISTORY.createdAt().desc()).select(HISTORY.id()).execute();
        if (ids.size() > count) {
            sql.createDelete(HISTORY).where(HISTORY.id().in(ids.subList(count, ids.size()))).execute();
        }
    }

    /** 为指定主体初始化 TOTP：生成密钥并签发 10 个一次性备用码，返回设置信息。 */
    @Transactional
    public SetupView setupTotp(String subjectType, UUID subject, UUID tenant, String label) {
        var old = findFactor(subjectType, subject, "totp");
        if (old != null && old.enabled()) {
            throw new DomainException(ErrorCodeConstants.MFA_ALREADY_ENABLED);
        }
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
        return new SetupView(secret, "otpauth://totp/Easy1Auth:" + url(label) + "?secret=" + secret + "&issuer=Easy1Auth&digits=6&period=30", recovery);
    }

    /** 启用 TOTP：校验一次性验证码通过后置 enabled 并记录当前时间步。 */
    @Transactional
    public void enableTotp(String subjectType, UUID subject, String code) {
        var f = requireFactor(subjectType, subject, "totp");
        String secret = cipher.decrypt(aad(subjectType, subject, "totp"), f.encryptedSecret());
        if (!totp.verify(secret, code, Instant.now(), f.lastTotpStep())) {
            throw new DomainException(ErrorCodeConstants.MFA_CODE_INVALID);
        }
        sql.createUpdate(FACTOR).set(FACTOR.enabled(), true).set(FACTOR.lastTotpStep(), totp.step(Instant.now())).set(FACTOR.updatedAt(), Instant.now()).where(FACTOR.id().eq(f.id())).execute();
    }

    /** 关闭 TOTP：校验验证码通过后删除该主体的全部认证因子。 */
    @Transactional
    public void disable(String subjectType, UUID subject, String code) {
        verifyTotp(subjectType, subject, code);
        sql.createDelete(FACTOR).where(FACTOR.subjectType().eq(subjectType), FACTOR.subjectId().eq(subject)).execute();
    }

    /** 校验 TOTP 验证码：未启用或验证失败均抛异常，成功后防重放更新时间步。 */
    @Transactional
    public boolean verifyTotp(String subjectType, UUID subject, String code) {
        var f = requireFactor(subjectType, subject, "totp");
        if (!f.enabled()) {
            throw new DomainException(ErrorCodeConstants.MFA_NOT_ENABLED);
        }
        String secret = cipher.decrypt(aad(subjectType, subject, "totp"), f.encryptedSecret());
        Instant now = Instant.now();
        if (!totp.verify(secret, code, now, f.lastTotpStep())) {
            throw new DomainException(ErrorCodeConstants.MFA_CODE_INVALID);
        }
        sql.createUpdate(FACTOR).set(FACTOR.lastTotpStep(), totp.step(now)).set(FACTOR.updatedAt(), now).where(FACTOR.id().eq(f.id()), Predicate.or(FACTOR.lastTotpStep().isNull(), FACTOR.lastTotpStep().lt(totp.step(now)))).execute();
        return true;
    }

    /** 签发邮箱验证码挑战（用于登录等常规场景，60 秒内同主体/目的地限发一次）。 */
    @Transactional
    public ChallengeView issueEmailChallenge(String subjectType, UUID subject, UUID tenant, String purpose) {
        return issueEmailChallenge(subjectType, subject, tenant, purpose, null);
    }

    /**
     * 签发邮箱验证码挑战。
     *
     * @param destination 发送目的地（邮箱），在 subject 为 null 时用于限流判断
     */
    @Transactional
    public ChallengeView issueEmailChallenge(String subjectType, UUID subject, UUID tenant, String purpose, String destination) {
        Instant now = Instant.now();
        var recentQuery = sql.createQuery(CHALLENGE).where(CHALLENGE.subjectType().eq(subjectType), CHALLENGE.purpose().eq(purpose), CHALLENGE.factorType().eq("email"), CHALLENGE.createdAt().gt(now.minusSeconds(60)));
        if (subject != null) {
            recentQuery.where(CHALLENGE.subjectId().eq(subject));
        } else if (destination != null) {
            recentQuery.where(CHALLENGE.destination().eq(destination));
        }
        if (recentQuery.select(CHALLENGE.id()).exists()) {
            throw new DomainException(ErrorCodeConstants.CODE_RATE_LIMITED);
        }
        String token = randomToken(32), code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        var e = AuthenticationChallengeEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTokenHash(hash(token)).setSubjectType(subjectType).setSubjectId(subject).setTenantId(tenant).setPurpose(purpose).setDestination(destination).setFactorType("email").setCodeHash(hash(code)).setAttempts(0).setMaxAttempts(5).setExpiresAt(now.plusSeconds(600)).setConsumedAt(null).setCreatedAt(now).setLastSentAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return new ChallengeView(token, code, 600);
    }

    /** 生成一个伪挑战 token（不落库），用于防探测的应答混淆。 */
    public String decoyChallengeToken() {
        return randomToken(32);
    }

    /** 消费邮箱验证码挑战：校验过期/次数/重放后返回已确认的主体与目的地。 */
    @Transactional
    public ConsumedEmailChallengeView consumeEmailChallenge(String token, String code, String subjectType, String purpose) {
        var row = sql.createQuery(CHALLENGE).where(CHALLENGE.tokenHash().eq(hash(token)), CHALLENGE.subjectType().eq(subjectType), CHALLENGE.purpose().eq(purpose), CHALLENGE.factorType().eq("email")).select(CHALLENGE).forUpdate().fetchOneOrNull();
        if (row == null || row.subjectId() == null || row.consumedAt() != null || row.expiresAt().isBefore(Instant.now()) || row.attempts() >= row.maxAttempts()) {
            throw new DomainException(ErrorCodeConstants.MFA_CHALLENGE_INVALID);
        }
        if (row.codeHash() == null || !MessageDigest.isEqual(hash(code).getBytes(StandardCharsets.US_ASCII), row.codeHash().getBytes(StandardCharsets.US_ASCII))) {
            sql.createUpdate(CHALLENGE).set(CHALLENGE.attempts(), CHALLENGE.attempts().plus(1)).where(CHALLENGE.id().eq(row.id())).execute();
            throw new DomainException(ErrorCodeConstants.MFA_CODE_INVALID);
        }
        if (sql.createUpdate(CHALLENGE).set(CHALLENGE.consumedAt(), Instant.now()).where(CHALLENGE.id().eq(row.id()), CHALLENGE.consumedAt().isNull()).execute() != 1) {
            throw new DomainException(ErrorCodeConstants.MFA_CHALLENGE_REPLAYED);
        }
        return new ConsumedEmailChallengeView(row.subjectId(), row.destination());
    }

    /**
     * 消费注册场景的邮箱验证码挑战。
     *
     * <p>与 {@link #consumeEmailChallenge} 的区别在于注册场景下 {@code subject_id}
     * 为 null（用户尚未创建），因此不校验该字段。其余校验（过期、次数、重放）
     * 与普通邮箱挑战一致。</p>
     */
    @Transactional
    public ConsumedEmailChallengeView consumeRegistrationEmailChallenge(String token, String code, String purpose) {
        var row = sql.createQuery(CHALLENGE).where(CHALLENGE.tokenHash().eq(hash(token)), CHALLENGE.subjectType().eq("registration"), CHALLENGE.purpose().eq(purpose), CHALLENGE.factorType().eq("email")).select(CHALLENGE).forUpdate().fetchOneOrNull();
        if (row == null || row.consumedAt() != null || row.expiresAt().isBefore(Instant.now()) || row.attempts() >= row.maxAttempts()) {
            throw new DomainException(ErrorCodeConstants.MFA_CHALLENGE_INVALID);
        }
        if (row.codeHash() == null || !MessageDigest.isEqual(hash(code).getBytes(StandardCharsets.US_ASCII), row.codeHash().getBytes(StandardCharsets.US_ASCII))) {
            sql.createUpdate(CHALLENGE).set(CHALLENGE.attempts(), CHALLENGE.attempts().plus(1)).where(CHALLENGE.id().eq(row.id())).execute();
            throw new DomainException(ErrorCodeConstants.MFA_CODE_INVALID);
        }
        if (sql.createUpdate(CHALLENGE).set(CHALLENGE.consumedAt(), Instant.now()).where(CHALLENGE.id().eq(row.id()), CHALLENGE.consumedAt().isNull()).execute() != 1) {
            throw new DomainException(ErrorCodeConstants.MFA_CHALLENGE_REPLAYED);
        }
        return new ConsumedEmailChallengeView(null, row.destination());
    }

    /** 签发 TOTP 挑战（不存验证码，验证码在消费时实时计算比对）。 */
    @Transactional
    public ChallengeView issueTotpChallenge(String subjectType, UUID subject, UUID tenant, String purpose) {
        var factor = requireFactor(subjectType, subject, "totp");
        if (!factor.enabled()) {
            throw new DomainException(ErrorCodeConstants.MFA_NOT_ENABLED);
        }
        String token = randomToken(32);
        Instant now = Instant.now();
        var e = AuthenticationChallengeEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTokenHash(hash(token)).setSubjectType(subjectType).setSubjectId(subject).setTenantId(tenant).setPurpose(purpose).setFactorType("totp").setCodeHash(null).setAttempts(0).setMaxAttempts(5).setExpiresAt(now.plusSeconds(600)).setConsumedAt(null).setCreatedAt(now).setLastSentAt(null));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return new ChallengeView(token, null, 600);
    }

    /** 消费 TOTP 挑战：实时校验验证码成功后标记已用，返回被认证的主体 ID。 */
    @Transactional
    public UUID consumeTotpChallenge(String token, String code, String subjectType, String purpose) {
        var row = sql.createQuery(CHALLENGE).where(CHALLENGE.tokenHash().eq(hash(token)), CHALLENGE.subjectType().eq(subjectType), CHALLENGE.purpose().eq(purpose), CHALLENGE.factorType().eq("totp")).select(CHALLENGE).forUpdate().fetchOneOrNull();
        if (row == null || row.consumedAt() != null || row.expiresAt().isBefore(Instant.now()) || row.attempts() >= row.maxAttempts() || row.subjectId() == null) {
            throw new DomainException(ErrorCodeConstants.MFA_CHALLENGE_INVALID);
        }
        try {
            verifyTotp(row.subjectType(), row.subjectId(), code);
        } catch (DomainException ex) {
            sql.createUpdate(CHALLENGE).set(CHALLENGE.attempts(), CHALLENGE.attempts().plus(1)).where(CHALLENGE.id().eq(row.id())).execute();
            throw ex;
        }
        if (sql.createUpdate(CHALLENGE).set(CHALLENGE.consumedAt(), Instant.now()).where(CHALLENGE.id().eq(row.id()), CHALLENGE.consumedAt().isNull()).execute() != 1) {
            throw new DomainException(ErrorCodeConstants.MFA_CHALLENGE_REPLAYED);
        }
        return row.subjectId();
    }

    /** 查询主体已启用的多因素认证方式列表。 */
    @Transactional(readOnly = true)
    public StatusView status(String type, UUID subject) {
        var rows = sql.createQuery(FACTOR).where(FACTOR.subjectType().eq(type), FACTOR.subjectId().eq(subject), FACTOR.enabled().eq(true)).select(FACTOR.factorType()).execute();
        return new StatusView(!rows.isEmpty(), rows);
    }

    /** 查询指定主体的某类认证因子（不存在返回 null）。 */
    private AuthenticationFactorEntity findFactor(String type, UUID subject, String factor) {
        return sql.createQuery(FACTOR).where(FACTOR.subjectType().eq(type), FACTOR.subjectId().eq(subject), FACTOR.factorType().eq(factor)).select(FACTOR).fetchOneOrNull();
    }

    /** 查询指定主体的某类认证因子，未配置时抛出异常。 */
    private AuthenticationFactorEntity requireFactor(String type, UUID subject, String factor) {
        var f = findFactor(type, subject, factor);
        if (f == null) {
            throw new DomainException(ErrorCodeConstants.MFA_NOT_CONFIGURED);
        }
        return f;
    }

    /** 读取全局安全策略，不存在时按默认值初始化一条。 */
    private SecurityPolicyEntity policyEntity() {
        var found = sql.createQuery(POLICY).select(POLICY).fetchOneOrNull();
        if (found == null) {
            found = SecurityPolicyEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setPasswordMinLength(8).setPasswordRequireUpper(true).setPasswordRequireLower(true).setPasswordRequireNumber(true).setPasswordRequireSpecial(true).setPasswordMaxAgeDays(90).setPasswordHistoryCount(5).setMfaRequired(false).setLoginAttemptLimit(5).setLockoutDurationSeconds(1800).setUpdatedAt(Instant.now()));
            sql.saveCommand(found).setMode(SaveMode.INSERT_IF_ABSENT).execute();
        }
        return found;
    }

    /** 读取指定租户的安全策略，不存在时按默认值初始化一条。 */
    private SecurityPolicyEntity policyEntity(UUID tenant) {
        var found = sql.createQuery(POLICY).where(POLICY.tenantId().eq(tenant)).select(POLICY).fetchOneOrNull();
        if (found == null) {
            found = SecurityPolicyEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setPasswordMinLength(8).setPasswordRequireUpper(true).setPasswordRequireLower(true).setPasswordRequireNumber(true).setPasswordRequireSpecial(true).setPasswordMaxAgeDays(90).setPasswordHistoryCount(5).setMfaRequired(false).setLoginAttemptLimit(5).setLockoutDurationSeconds(1800).setUpdatedAt(Instant.now()));
            sql.saveCommand(found).setMode(SaveMode.INSERT_IF_ABSENT).execute();
        }
        return found;
    }

    /** 将策略实体转为视图对象。 */
    private static PolicyView view(SecurityPolicyEntity e) {
        return new PolicyView(e.passwordMinLength(), e.passwordRequireUpper(), e.passwordRequireLower(), e.passwordRequireNumber(), e.passwordRequireSpecial(), e.passwordMaxAgeDays(), e.passwordHistoryCount(), e.mfaRequired(), e.loginAttemptLimit(), e.lockoutDurationSeconds());
    }

    /** 校验策略参数取值范围：密码长度 8-128、历史 0-24、锁定时长等下限约束。 */
    private static void validate(PolicyView p) {
        if (p == null || p.minLength() < 8 || p.minLength() > 128 || p.historyCount() < 0 || p.historyCount() > 24 || p.loginAttemptLimit() < 1 || p.lockoutSeconds() < 60) {
            throw new DomainException(ErrorCodeConstants.SECURITY_POLICY_INVALID);
        }
    }

    /** 拼装加密用的 AAD：主体类型 + 主体 ID + 因子类型。 */
    private static String aad(String type, UUID id, String factor) {
        return type + ":" + id + ":" + factor;
    }

    /** 对标签做 URL 编码，用于拼装 otpauth 二维码 URI。 */
    private static String url(String v) {
        return java.net.URLEncoder.encode(v == null ? "user" : v, StandardCharsets.UTF_8);
    }

    /** 生成指定字节数随机数的 URL-safe Base64 token（挑战 token / 备用码）。 */
    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        random.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    /** SHA-256 十六进制哈希（用于验证码/token 的落库比对）。 */
    private static String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Objects.toString(value, "").getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    

    

    

    

    
}
