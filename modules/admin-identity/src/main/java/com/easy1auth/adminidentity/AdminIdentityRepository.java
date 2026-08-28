package com.easy1auth.adminidentity;

import com.easy1auth.adminidentity.model.*;
import com.easy1auth.infrastructure.foundation.id.UuidV7;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

/**
 * 管理账号身份数据访问仓储（包私有）。
 *
 * <p>封装对 admin_account / admin_credential / admin_refresh_session 表的读写，
 * 通过 jimmer {@link JSqlClient} 完成查询与更新；注册码的原生 SQL 交由
 * {@link RegistrationCodeNativeSql} 处理。会话轮换等并发安全场景使用
 * {@code forUpdate} 行级锁。</p>
 */
@Repository
class AdminIdentityRepository {
    /** admin_account 表静态描述符 */
    private static final AdminAccountEntityTable ACCOUNT = AdminAccountEntityTable.$;
    /** admin_credential 表静态描述符 */
    private static final AdminCredentialEntityTable CREDENTIAL = AdminCredentialEntityTable.$;
    /** admin_refresh_session 表静态描述符 */
    private static final AdminRefreshSessionEntityTable SESSION = AdminRefreshSessionEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;
    /** 注册码原生 SQL 组件 */
    private final RegistrationCodeNativeSql registrationCodes;

    AdminIdentityRepository(JSqlClient sql, RegistrationCodeNativeSql registrationCodes) {
        this.sql = sql;
        this.registrationCodes = registrationCodes;
    }

    /** 按登录标识（用户名或邮箱，忽略大小写）查询账号及其密码哈希，用于登录校验。 */
    Optional<CredentialRow> findCredential(String login) {
        var account = sql.createQuery(ACCOUNT).where(Predicate.or(ACCOUNT.username().lower().eq(login), ACCOUNT.email().lower().eq(login))).select(ACCOUNT).fetchOptional();
        if (account.isEmpty()) {
            return Optional.empty();
        }
        var credential = sql.createQuery(CREDENTIAL).where(CREDENTIAL.accountId().eq(account.get().id())).select(CREDENTIAL.passwordHash()).fetchOptional();
        return credential.map(hash -> new CredentialRow(toDomain(account.get()), hash));
    }

    /** 按 ID 查询 active 状态的管理账号。 */
    Optional<AdminAccount> findActive(UUID id) {
        return sql.createQuery(ACCOUNT).where(ACCOUNT.id().eq(id), ACCOUNT.status().eq("active")).select(ACCOUNT).fetchOptional().map(AdminIdentityRepository::toDomain);
    }

    /** 按邮箱（忽略大小写）查询 active 状态的管理账号。 */
    Optional<AdminAccount> findActiveByEmail(String email) {
        return sql.createQuery(ACCOUNT).where(ACCOUNT.email().lower().eq(email), ACCOUNT.status().eq("active")).select(ACCOUNT).fetchOptional().map(AdminIdentityRepository::toDomain);
    }

    /** 按 ID 加行级锁查询 active 状态的管理账号（并发保护）。 */
    Optional<AdminAccount> lockActive(UUID id) {
        return sql.createQuery(ACCOUNT).where(ACCOUNT.id().eq(id), ACCOUNT.status().eq("active")).select(ACCOUNT).forUpdate().fetchOptional().map(AdminIdentityRepository::toDomain);
    }

    /** 判断用户名或邮箱是否已被注册（忽略大小写）。 */
    boolean exists(String username, String email) {
        return sql.createQuery(ACCOUNT).where(Predicate.or(ACCOUNT.username().lower().eq(username), ACCOUNT.email().lower().eq(email))).select(ACCOUNT.id()).exists();
    }

    /** 创建 active 状态的管理账号及其初始凭证（安全版本号初始为 1），返回领域模型。 */
    AdminAccount create(String username, String email, String hash) {
        UUID id = UuidV7.randomUuid();
        Instant now = Instant.now();
        var entity = AdminAccountEntityDraft.$.produce(d -> d.setId(id).setUsername(username).setEmail(email).setPhone(null).setStatus("active").setLastTenantId(null).setSecurityVersion(1).setMfaEnabled(false).setMfaType(null).setLastLoginAt(null).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
        sql.saveCommand(AdminCredentialEntityDraft.$.produce(d -> d.setAccountId(id).setPasswordHash(hash).setPasswordChangedAt(now).setCreatedAt(now).setUpdatedAt(now))).setMode(SaveMode.INSERT_ONLY).execute();
        return toDomain(entity);
    }

    /** 原子消费注册码（委托给原生 SQL 组件），成功消费返回 true。 */
    boolean consumeRegistrationCode(String email, String hash) {
        return registrationCodes.consume(email, hash);
    }

    /** 签发注册码（委托给原生 SQL 组件，签发前清理该邮箱旧码及过期记录）。 */
    void issueRegistrationCode(UUID id, String email, String hash, Instant expires) {
        registrationCodes.issue(id, email, hash, expires);
    }

    /** 记录账号最近登录时间。 */
    void recordLogin(UUID id) {
        sql.createUpdate(ACCOUNT).set(ACCOUNT.lastLoginAt(), Instant.now()).set(ACCOUNT.updatedAt(), Instant.now()).where(ACCOUNT.id().eq(id)).execute();
    }

    /** 创建刷新会话（仅保存令牌哈希与签发元信息，不落库令牌原文）。 */
    void createSession(UUID id, UUID account, String hash, long version, Instant expires, String agent, String ip) {
        Instant now = Instant.now();
        sql.saveCommand(AdminRefreshSessionEntityDraft.$.produce(d -> d.setId(id).setAccountId(account).setTokenHash(hash).setSecurityVersion(version).setExpiresAt(expires).setRevokedAt(null).setReplacedBy(null).setCreatedAt(now).setLastUsedAt(null).setUserAgent(agent).setIpAddress(ip))).setMode(SaveMode.INSERT_ONLY).execute();
    }

    /** 按令牌哈希加行级锁查询未吊销且未过期的刷新会话及其所属账号。 */
    Optional<SessionRow> lockSession(String hash) {
        var session = sql.createQuery(SESSION).where(SESSION.tokenHash().eq(hash), SESSION.revokedAt().isNull(), SESSION.expiresAt().gt(Instant.now())).select(SESSION).forUpdate().fetchOptional();
        if (session.isEmpty()) {
            return Optional.empty();
        }
        return findAccount(session.get().accountId()).map(a -> new SessionRow(session.get().id(), session.get().securityVersion(), a));
    }

    /** 轮换刷新会话：吊销旧会话并记录替换的新会话 ID。 */
    void rotate(UUID oldId, UUID replacement) {
        sql.createUpdate(SESSION).set(SESSION.revokedAt(), Instant.now()).set(SESSION.lastUsedAt(), Instant.now()).set(SESSION.replacedBy(), replacement).where(SESSION.id().eq(oldId), SESSION.revokedAt().isNull()).execute();
    }

    /** 按令牌哈希吊销刷新会话（用于退出登录）。 */
    void revoke(String hash) {
        sql.createUpdate(SESSION).set(SESSION.revokedAt(), Instant.now()).where(SESSION.tokenHash().eq(hash), SESSION.revokedAt().isNull()).execute();
    }

    /** 吊销某账号的全部刷新会话。 */
    void revokeAll(UUID account) {
        sql.createUpdate(SESSION).set(SESSION.revokedAt(), Instant.now()).where(SESSION.accountId().eq(account), SESSION.revokedAt().isNull()).execute();
    }

    /** 使某账号全部会话失效：递增安全版本号并吊销所有会话。 */
    void invalidateAccountSessions(UUID account) {
        incrementVersion(account);
        revokeAll(account);
    }

    /** 更新账号资料（仅更新传入的非空字段），返回更新后的账号。 */
    AdminAccount updateProfile(UUID id, String username, String email, String phone) {
        var update = sql.createUpdate(ACCOUNT).set(ACCOUNT.updatedAt(), Instant.now()).where(ACCOUNT.id().eq(id));
        if (username != null) {
            update.set(ACCOUNT.username(), username);
        }
        if (email != null) {
            update.set(ACCOUNT.email(), email);
        }
        if (phone != null) {
            update.set(ACCOUNT.phone(), phone);
        }
        if (update.execute() != 1) {
            throw new IllegalStateException("account missing");
        }
        return findActive(id).orElseThrow();
    }

    /** 判断邮箱是否已被除指定账号外的其他账号使用。 */
    boolean emailExistsForOtherAccount(UUID id, String email) {
        return sql.createQuery(ACCOUNT).where(ACCOUNT.id().ne(id), ACCOUNT.email().lower().eq(email)).select(ACCOUNT.id()).exists();
    }

    /** 更新当前账号自己的用户名与手机号，返回更新后的账号。 */
    AdminAccount updateOwnProfile(UUID id, String username, String phone) {
        if (sql.createUpdate(ACCOUNT)
                .set(ACCOUNT.username(), username)
                .set(ACCOUNT.phone(), phone)
                .set(ACCOUNT.updatedAt(), Instant.now())
                .where(ACCOUNT.id().eq(id), ACCOUNT.status().eq("active"))
                .execute() != 1) {
            throw new IllegalStateException("account missing");
        }
        return findActive(id).orElseThrow();
    }

    /** 修改当前账号邮箱并递增安全版本号，同时吊销全部会话。 */
    AdminAccount changeOwnEmail(UUID id, String email) {
        if (sql.createUpdate(ACCOUNT)
                .set(ACCOUNT.email(), email)
                .set(ACCOUNT.securityVersion(), ACCOUNT.securityVersion().plus(1L))
                .set(ACCOUNT.updatedAt(), Instant.now())
                .where(ACCOUNT.id().eq(id), ACCOUNT.status().eq("active"))
                .execute() != 1) {
            throw new IllegalStateException("account missing");
        }
        revokeAll(id);
        return findActive(id).orElseThrow();
    }

    /** 更新账号状态（active/disabled）并递增安全版本号。 */
    AdminAccount updateStatus(UUID id, String status) {
        sql.createUpdate(ACCOUNT).set(ACCOUNT.status(), status).set(ACCOUNT.securityVersion(), ACCOUNT.securityVersion().plus(1L)).set(ACCOUNT.updatedAt(), Instant.now()).where(ACCOUNT.id().eq(id)).execute();
        return findAccount(id).orElseThrow();
    }

    /** 重置账号密码并递增安全版本号（使旧令牌/会话失效）。 */
    void resetPassword(UUID id, String hash) {
        sql.createUpdate(CREDENTIAL).set(CREDENTIAL.passwordHash(), hash).set(CREDENTIAL.passwordChangedAt(), Instant.now()).set(CREDENTIAL.updatedAt(), Instant.now()).where(CREDENTIAL.accountId().eq(id)).execute();
        incrementVersion(id);
    }

    /** 关闭账号 MFA 并递增安全版本号。 */
    AdminAccount resetMfa(UUID id) {
        sql.createUpdate(ACCOUNT).set(ACCOUNT.mfaEnabled(), false).set(ACCOUNT.mfaType(), (String) null).set(ACCOUNT.securityVersion(), ACCOUNT.securityVersion().plus(1L)).set(ACCOUNT.updatedAt(), Instant.now()).where(ACCOUNT.id().eq(id)).execute();
        return findAccount(id).orElseThrow();
    }

    /** 开启账号 MFA 并递增安全版本号。 */
    AdminAccount enableMfa(UUID id, String type) {
        sql.createUpdate(ACCOUNT).set(ACCOUNT.mfaEnabled(), true).set(ACCOUNT.mfaType(), type).set(ACCOUNT.securityVersion(), ACCOUNT.securityVersion().plus(1L)).set(ACCOUNT.updatedAt(), Instant.now()).where(ACCOUNT.id().eq(id)).execute();
        return findAccount(id).orElseThrow();
    }

    /** 递增账号安全版本号（使旧令牌/会话失效）。 */
    private void incrementVersion(UUID id) {
        sql.createUpdate(ACCOUNT).set(ACCOUNT.securityVersion(), ACCOUNT.securityVersion().plus(1L)).set(ACCOUNT.updatedAt(), Instant.now()).where(ACCOUNT.id().eq(id)).execute();
    }

    /** 按 ID 查询账号（不限状态）。 */
    private Optional<AdminAccount> findAccount(UUID id) {
        return sql.createQuery(ACCOUNT).where(ACCOUNT.id().eq(id)).select(ACCOUNT).fetchOptional().map(AdminIdentityRepository::toDomain);
    }

    /** 将账号实体转换为领域模型。 */
    private static AdminAccount toDomain(AdminAccountEntity a) {
        return new AdminAccount(a.id(), a.username(), a.email(), a.phone(), a.status(), a.securityVersion(), a.lastTenantId(), a.mfaEnabled(), a.mfaType(), a.lastLoginAt(), a.createdAt(), a.updatedAt());
    }

    /**
     * 凭证查询结果。
     *
     * @param account      账号信息
     * @param passwordHash 密码哈希
     */
    record CredentialRow(AdminAccount account, String passwordHash) {
    }

    /**
     * 会话查询结果。
     *
     * @param id                     会话 ID
     * @param sessionSecurityVersion 签发时的账号安全版本号
     * @param account                会话所属账号
     */
    record SessionRow(UUID id, long sessionSecurityVersion, AdminAccount account) {
    }
}
