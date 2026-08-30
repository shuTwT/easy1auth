package com.easy1auth.security.service;

import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.id.UuidV7;
import com.easy1auth.security.ErrorCodeConstants;
import com.easy1auth.security.model.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * 登录保护服务：基于失败次数限制实现账号/密钥的临时锁定与解锁。
 *
 * <p>以「主体类型 + 主体键 + 租户」为维度记录失败次数，连续失败达到阈值后
 * 锁定一段时间；登录成功或锁定期过后自动恢复。支持对登录名、设备、验证码
 * 等多种主体分别计数。</p>
 */
@Service
public class LoginProtectionService {
    /** login_failure_state 表静态描述符 */
    private static final LoginFailureStateEntityTable STATE = LoginFailureStateEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;

    public LoginProtectionService(JSqlClient sql) {
        this.sql = sql;
    }

    /** 校验是否被临时锁定：在锁定期内则抛出 {@link ErrorCodeConstants#ACCOUNT_TEMPORARILY_LOCKED}。 */
    @Transactional(readOnly = true)
    public void assertAllowed(String type, String key, UUID tenant) {
        var row = find(type, normalize(key), tenant, false);
        if (row != null && row.lockedUntil() != null && row.lockedUntil().isAfter(Instant.now())) {
            throw new DomainException(ErrorCodeConstants.ACCOUNT_TEMPORARILY_LOCKED);
        }
    }

    /** 记录一次失败：递增失败次数，达到阈值即锁定 {@code lockSeconds} 秒。 */
    @Transactional
    public void failed(String type, String key, UUID tenant, int limit, int lockSeconds) {
        String normalized = normalize(key);
        var row = find(type, normalized, tenant, true);
        Instant now = Instant.now();
        if (row == null) {
            var e = LoginFailureStateEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setSubjectType(type).setSubjectKey(normalized).setTenantId(tenant).setFailedAttempts(1).setLockedUntil(limit <= 1 ? now.plusSeconds(lockSeconds) : null).setLastFailedAt(now));
            sql.saveCommand(e).setMode(SaveMode.INSERT_IF_ABSENT).execute();
            return;
        }
        int failures = (row.lockedUntil() != null && row.lockedUntil().isBefore(now) ? 0 : row.failedAttempts()) + 1;
        sql.createUpdate(STATE).set(STATE.failedAttempts(), failures).set(STATE.lastFailedAt(), now).set(STATE.lockedUntil(), failures >= limit ? now.plusSeconds(lockSeconds) : null).where(STATE.id().eq(row.id())).execute();
    }

    /** 记录一次成功登录：清除该主体的失败计数记录。 */
    @Transactional
    public void succeeded(String type, String key, UUID tenant) {
        sql.createDelete(STATE).where(STATE.subjectType().eq(type), STATE.subjectKey().eq(normalize(key)), tenant == null ? STATE.tenantId().isNull() : STATE.tenantId().eq(tenant)).execute();
    }

    /** 查询失败状态记录，lock 为 true 时加行级锁（配合事务内的计数更新）。 */
    private LoginFailureStateEntity find(String type, String key, UUID tenant, boolean lock) {
        var q = sql.createQuery(STATE).where(STATE.subjectType().eq(type), STATE.subjectKey().eq(key), tenant == null ? STATE.tenantId().isNull() : STATE.tenantId().eq(tenant)).select(STATE);
        return lock ? q.forUpdate().fetchOneOrNull() : q.fetchOneOrNull();
    }

    /** 归一化主体键：去除首尾空白并转小写，截断至 400 字符，避免超长键入库。 */
    private static String normalize(String key) {
        return Objects.toString(key, "").strip().toLowerCase(Locale.ROOT).substring(0, Math.min(400, Objects.toString(key, "").strip().length()));
    }
}
