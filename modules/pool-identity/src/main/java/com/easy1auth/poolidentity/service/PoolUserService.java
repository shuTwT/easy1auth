package com.easy1auth.poolidentity.service;

import com.easy1auth.poolidentity.constant.ErrorCodeConstants;
import com.easy1auth.poolidentity.dto.PoolUserView;
import com.easy1auth.poolidentity.dto.PoolUserInput;
import com.easy1auth.poolidentity.dto.RecentLogin;
import com.easy1auth.poolidentity.dto.UserStats;
import com.easy1auth.poolidentity.model.PoolUserEntity;
import com.easy1auth.poolidentity.model.PoolUserEntityDraft;
import com.easy1auth.poolidentity.model.PoolUserEntityTable;
import com.easy1auth.infrastructure.foundation.util.TenantContextHolder;
import com.easy1auth.security.service.SecurityPolicyService;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.id.UuidV7;
import com.easy1auth.infrastructure.foundation.web.PageData;
import com.easy1auth.tenant.service.TenantService;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * pool_user 目录服务：管理第三方接入用户的生命周期与档案信息。
 *
 * <p>覆盖用户的创建、查询、更新、删除、状态变更与密码管理，并结合
 * {@link TenantService} 做用户配额校验、{@link SecurityPolicyService}
 * 做密码强度与历史复用校验。pool_user 供第三方授权登录使用，
 * 不属于管理端（admin_user）账号体系。</p>
 */
@Service
public class PoolUserService {
    /** pool_user 用户表静态描述符 */
    private static final PoolUserEntityTable USER = PoolUserEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;
    /** 租户服务（用于锁定租户并校验用户配额上限） */
    private final TenantService tenants;
    /** 密码编码器（用于密码加密与比对） */
    private final PasswordEncoder passwords;
    /** 安全策略服务（密码强度、历史复用校验） */
    private final SecurityPolicyService security;

    public PoolUserService(JSqlClient sql, TenantService tenants, PasswordEncoder passwords, SecurityPolicyService security) {
        this.sql = sql;
        this.tenants = tenants;
        this.passwords = passwords;
        this.security = security;
    }

    /** 分页查询租户下的用户，可按用户名/邮箱/手机号/姓名/状态/部门过滤，按创建时间倒序。 */
    @Transactional(readOnly = true)
    public PageData<PoolUserView> list(UUID tenant, int page, int pageSize, String username, String email, String phone, String name, String status, String department) {
        int p = Math.max(1, page), size = Math.min(100, Math.max(1, pageSize));
        var q = sql.createQuery(USER).where(USER.tenantId().eq(tenant)).whereIf(username != null, () -> USER.username().ilike(username, LikeMode.ANYWHERE)).whereIf(email != null, () -> USER.email().ilike(email, LikeMode.ANYWHERE)).whereIf(phone != null, () -> USER.phone().ilike(phone, LikeMode.ANYWHERE)).whereIf(name != null, () -> USER.name().ilike(name, LikeMode.ANYWHERE)).whereIf(status != null, () -> USER.status().eq(status)).whereIf(department != null, () -> USER.department().eq(department)).orderBy(USER.createdAt().desc()).select(USER);
        long total = q.fetchUnlimitedCount();
        return PageData.of(q.limit(size, (long) (p - 1) * size).execute().stream().map(this::view).toList(), p, size, total);
    }

    /** 查询指定租户下单个用户的详情视图。 */
    @Transactional(readOnly = true)
    public PoolUserView get(UUID tenant, UUID id) {
        return view(entity(tenant, id));
    }

    /**
     * 创建 pool_user 用户：校验必填字段与密码强度，并锁定租户校验用户配额上限。
     *
     * <p>默认状态为 active，邮箱、手机号验证标记初始为 false；密码非空时以
     * BCrypt 加密存储，为空则允许无密码（后续由第三方认证）。</p>
     */
    @Transactional
    public PoolUserView create(UUID tenant, PoolUserInput in) {
        validate(in.username(), in.email(), in.phone(), in.name());
        if (in.password() != null) {
            security.validatePassword(in.password(), security.policy(tenant));
        }
        int limit = tenants.lockForUserQuota(tenant);
        long count = sql.createQuery(USER).where(USER.tenantId().eq(tenant)).select(USER.id()).fetchUnlimitedCount();
        if (count >= limit) {
            throw new DomainException(ErrorCodeConstants.TENANT_USER_LIMIT);
        }
        Instant now = Instant.now();
        String email = normalizeEmail(in.email());
        var e = PoolUserEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setUsername(in.username().strip()).setEmail(email).setPhone(in.phone()).setPasswordHash(in.password() == null ? null : passwords.encode(in.password())).setName(in.name().strip()).setAvatar(in.avatar()).setStatus("active").setEmailVerified(false).setPhoneVerified(false).setDepartment(in.department()).setPosition(in.position()).setCustomAttributes(in.customAttributes()).setLastLoginAt(null).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return view(e);
    }

    /** 更新用户档案：仅更新传入的非空字段；企业身份源托管的用户不可修改。 */
    @Transactional
    public PoolUserView update(UUID tenant, UUID id, PoolUserInput in) {
        var existing = entity(tenant, id);
        rejectEnterpriseManaged(existing);
        var u = sql.createUpdate(USER).set(USER.updatedAt(), Instant.now()).where(USER.id().eq(id), USER.tenantId().eq(tenant));
        if (in.username() != null) {
            u.set(USER.username(), in.username());
        }
        if (in.email() != null) {
            u.set(USER.email(), in.email().toLowerCase());
        }
        if (in.phone() != null) {
            u.set(USER.phone(), in.phone());
        }
        if (in.name() != null) {
            u.set(USER.name(), in.name());
        }
        if (in.avatar() != null) {
            u.set(USER.avatar(), in.avatar());
        }
        if (in.status() != null) {
            u.set(USER.status(), in.status());
        }
        if (in.department() != null) {
            u.set(USER.department(), in.department());
        }
        if (in.position() != null) {
            u.set(USER.position(), in.position());
        }
        if (in.customAttributes() != null) {
            u.set(USER.customAttributes(), in.customAttributes());
        }
        u.execute();
        return get(tenant, id);
    }

    /** 删除用户（硬删除）；企业身份源托管的用户不可删除。 */
    @Transactional
    public void delete(UUID tenant, UUID id) {
        rejectEnterpriseManaged(entity(tenant, id));
        int changed = sql.createDelete(USER).where(USER.id().eq(id), USER.tenantId().eq(tenant)).execute();
        if (changed != 1) {
            throw missing();
        }
    }

    /** 更新用户状态：active（正常）/ disabled（禁用）/ locked（锁定）。 */
    @Transactional
    public PoolUserView status(UUID tenant, UUID id, String status) {
        if (!Set.of("active", "disabled", "locked").contains(status)) {
            throw new DomainException(ErrorCodeConstants.USER_STATUS_INVALID);
        }
        rejectEnterpriseManaged(entity(tenant, id));
        sql.createUpdate(USER).set(USER.status(), status).set(USER.updatedAt(), Instant.now()).where(USER.id().eq(id), USER.tenantId().eq(tenant)).execute();
        return get(tenant, id);
    }

    /** 管理员重置用户密码：校验密码强度与历史复用后更新密码哈希。 */
    @Transactional
    public void resetPassword(UUID tenant, UUID id, String password) {
        var user = entity(tenant, id);
        var policy = security.policy(tenant);
        security.validatePassword(password, policy);
        security.rejectReusedPassword("pool_user", id, password, user.passwordHash(), passwords, policy.historyCount());
        sql.createUpdate(USER).set(USER.passwordHash(), passwords.encode(password)).set(USER.updatedAt(), Instant.now()).where(USER.id().eq(id), USER.tenantId().eq(tenant)).execute();
        security.rememberPassword("pool_user", id, user.passwordHash(), policy.historyCount());
    }

    /** 用户修改自己的密码：先校验原密码，再校验新密码强度与历史复用。 */
    @Transactional
    public void changePassword(UUID tenant, UUID id, String oldPassword, String newPassword) {
        var user = entity(tenant, id);
        if (user.passwordHash() == null || !passwords.matches(oldPassword, user.passwordHash())) {
            throw new DomainException(ErrorCodeConstants.CURRENT_PASSWORD_INVALID_POOL_USER);
        }
        var policy = security.policy(tenant);
        security.validatePassword(newPassword, policy);
        security.rejectReusedPassword("pool_user", id, newPassword, user.passwordHash(), passwords, policy.historyCount());
        sql.createUpdate(USER).set(USER.passwordHash(), passwords.encode(newPassword)).set(USER.updatedAt(), Instant.now()).where(USER.id().eq(id), USER.tenantId().eq(tenant)).execute();
        security.rememberPassword("pool_user", id, user.passwordHash(), policy.historyCount());
    }

    /** 统计租户下的用户总数及各状态（active / disabled / locked）数量。 */
    @Transactional(readOnly = true)
    public UserStats stats(UUID tenant) {
        var rows = sql.createQuery(USER).where(USER.tenantId().eq(tenant)).select(USER.status()).execute();
        return new UserStats(rows.size(), rows.stream().filter("active"::equals).count(), rows.stream().filter("disabled"::equals).count(), rows.stream().filter("locked"::equals).count());
    }

    private PoolUserEntity entity(UUID tenant, UUID id) {
        return sql.createQuery(USER).where(USER.id().eq(id), USER.tenantId().eq(tenant)).select(USER).fetchOptional().orElseThrow(this::missing);
    }

    private DomainException missing() {
        return new DomainException(ErrorCodeConstants.POOL_USER_NOT_FOUND);
    }

    /** 企业身份源托管的用户禁止在本系统修改，抛出领域异常。 */
    private static void rejectEnterpriseManaged(PoolUserEntity user) {
        if (user.enterpriseIdentitySourceId() != null) {
            throw new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_MANAGED_USER);
        }
    }

    private void validate(String u, String e, String p, String n) {
        if (u == null || u.isBlank() || n == null || n.isBlank() || (!present(e) && !present(p)) || (present(e) && !e.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new DomainException(ErrorCodeConstants.POOL_USER_INVALID);
        }
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeEmail(String email) {
        return present(email) ? email.strip().toLowerCase() : null;
    }

    private PoolUserView view(PoolUserEntity e) {
        return new PoolUserView(e.id(), e.tenantId(), e.username(), e.email(), e.phone(), e.name(), e.avatar(), e.status(), e.emailVerified(), e.phoneVerified(), e.department(), e.position(), e.customAttributes(), e.lastLoginAt(), e.createdAt(), e.updatedAt());
    }

    /** 从当前租户上下文取租户 ID 后分页查询用户列表。 */
    @Transactional(readOnly = true)
    public PageData<PoolUserView> list(int page, int pageSize, String username, String email, String phone, String name, String status, String department) {
        return list(TenantContextHolder.requireTenantId(), page, pageSize, username, email, phone, name, status, department);
    }

    /** 从当前租户上下文取租户 ID 后查询用户详情。 */
    @Transactional(readOnly = true)
    public PoolUserView get(UUID id) {
        return get(TenantContextHolder.requireTenantId(), id);
    }

    /** 从当前租户上下文取租户 ID 后创建用户。 */
    @Transactional
    public PoolUserView create(PoolUserInput in) {
        return create(TenantContextHolder.requireTenantId(), in);
    }

    /** 从当前租户上下文取租户 ID 后更新用户。 */
    @Transactional
    public PoolUserView update(UUID id, PoolUserInput in) {
        return update(TenantContextHolder.requireTenantId(), id, in);
    }

    /** 从当前租户上下文取租户 ID 后删除用户。 */
    @Transactional
    public void delete(UUID id) {
        delete(TenantContextHolder.requireTenantId(), id);
    }

    /** 从当前租户上下文取租户 ID 后更新用户状态。 */
    @Transactional
    public PoolUserView status(UUID id, String status) {
        return status(TenantContextHolder.requireTenantId(), id, status);
    }

    /** 从当前租户上下文取租户 ID 后重置用户密码。 */
    @Transactional
    public void resetPassword(UUID id, String password) {
        resetPassword(TenantContextHolder.requireTenantId(), id, password);
    }

    /** 从当前租户上下文取租户 ID 后修改用户密码。 */
    @Transactional
    public void changePassword(UUID id, String oldPassword, String newPassword) {
        changePassword(TenantContextHolder.requireTenantId(), id, oldPassword, newPassword);
    }

    /** 从当前租户上下文取租户 ID 后统计用户状态。 */
    @Transactional(readOnly = true)
    public UserStats stats() {
        return stats(TenantContextHolder.requireTenantId());
    }

    

    /**
     * 查询最近成功登录的用户列表（按登录时间倒序，最多 20 条）。
     *
     * <p>说明：身份模型未保留成功登录的来源 IP，调用方需将该项渲染为不可用。</p>
     */
    @Transactional(readOnly = true)
    public List<RecentLogin> recentLogins(int limit) {
        int size = Math.min(20, Math.max(1, limit));
        return sql.createQuery(USER)
                .where(USER.lastLoginAt().isNotNull())
                .orderBy(USER.lastLoginAt().desc())
                .select(USER)
                .limit(size)
                .execute()
                .stream()
                .map(user -> new RecentLogin(user.username(), user.email(), user.lastLoginAt()))
                .toList();
    }

    /** 统计自指定时间以来成功登录过的用户数（按最后登录时间 >= start 计数）。 */
    @Transactional(readOnly = true)
    public long successfulLoginCountSince(Instant start) {
        return sql.createQuery(USER)
                .where(USER.lastLoginAt().ge(start))
                .select(USER.id())
                .execute()
                .size();
    }

    

    
}
