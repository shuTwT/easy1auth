package com.easy1auth.tenant.infrastructure.repository;

import com.easy1auth.framework.common.id.UuidV7;
import com.easy1auth.framework.common.error.ErrorCode;
import com.easy1auth.tenant.constant.ErrorCodeConstants;
import com.easy1auth.tenant.dto.MembershipState;
import com.easy1auth.tenant.dto.UserPoolState;
import com.easy1auth.tenant.model.*;
import com.easy1auth.framework.common.error.DomainException;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

/**
 * 用户池数据访问仓储（包私有）。
 *
 * <p>封装对 tenant / tenant_membership / tenant_package 表的所有读写，
 * 通过 jimmer {@link JSqlClient} 完成查询与更新。需要并发安全的地方使用
 * {@code forUpdate} 行级锁，保证配额与管理员身份在并发下的一致性。</p>
 */
@Repository
public class UserPoolRepository {
    /** tenant 表静态描述符 */
    private static final UserPoolEntityTable USER_POOL = UserPoolEntityTable.$;
    /** tenant_membership 表静态描述符 */
    private static final UserPoolMembershipEntityTable MEMBERSHIP = UserPoolMembershipEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;

    public UserPoolRepository(JSqlClient sql) {
        this.sql = sql;
    }

    /** 查询账号名下处于 active 状态的用户池（含成员角色），按创建时间倒序。 */
    public List<UserPoolState> listForAccount(UUID accountId) {
        var memberships = sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(accountId), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).execute();
        if (memberships.isEmpty()) {
            return List.of();
        }
        var byUserPool = new HashMap<UUID, String>();
        memberships.forEach(m -> byUserPool.put(m.userPoolId(), m.membershipRole()));
        return sql.createQuery(USER_POOL).where(USER_POOL.id().in(byUserPool.keySet())).orderBy(USER_POOL.createdAt().desc()).select(USER_POOL.fetch(UserPoolEntityFetcher.$.allScalarFields().packageInfo(UserPoolPackageEntityFetcher.$.allScalarFields()))).execute().stream().map(t -> new UserPoolState(t, byUserPool.get(t.id()))).toList();
    }

    /** 查询全部普通用户池（非系统用户池），按创建时间倒序。 */
    public List<UserPoolEntity> listOrdinaryUserPools() {
        return sql.createQuery(USER_POOL).where(USER_POOL.system().eq(false)).orderBy(USER_POOL.createdAt().desc()).select(USER_POOL.fetch(UserPoolEntityFetcher.$.allScalarFields().packageInfo(UserPoolPackageEntityFetcher.$.allScalarFields()))).execute();
    }

    /** 查询全部用户池（含系统用户池），按创建时间倒序。 */
    public List<UserPoolEntity> listAllUserPools() {
        return sql.createQuery(USER_POOL).orderBy(USER_POOL.createdAt().desc()).select(USER_POOL.fetch(UserPoolEntityFetcher.$.allScalarFields().packageInfo(UserPoolPackageEntityFetcher.$.allScalarFields()))).execute();
    }

    /** 查询账号在某用户池的有效成员关系（含用户池的套餐与系统标记）；用户池非 active 时返回空。 */
    public Optional<MembershipState> activeMembership(UUID accountId, UUID userPoolId) {
        var membership = sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(accountId), MEMBERSHIP.userPoolId().eq(userPoolId), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).fetchOptional();
        if (membership.isEmpty()) {
            return Optional.empty();
        }
        var tenant = sql.createQuery(USER_POOL).where(USER_POOL.id().eq(userPoolId), USER_POOL.status().eq("active")).select(USER_POOL.fetch(UserPoolEntityFetcher.$.allScalarFields().packageInfo(UserPoolPackageEntityFetcher.$.allScalarFields()))).fetchOptional();
        if (tenant.isEmpty()) {
            return Optional.empty();
        }
        var userPoolPackage = tenant.get().packageInfo();
        return Optional.of(new MembershipState(membership.get().id(), membership.get().membershipRole(), tenant.get().isSystem(), userPoolPackage == null ? null : userPoolPackage.id()));
    }

    /** 按 ID 加行级锁查询激活中的用户池（用于安全材料生成前的并发保护）。 */
    public Optional<UserPoolEntity> lockActiveUserPool(UUID userPoolId) {
        return sql.createQuery(USER_POOL).where(USER_POOL.id().eq(userPoolId), USER_POOL.status().eq("active")).select(USER_POOL).forUpdate().fetchOptional();
    }

    /** 按 ID 加行级锁查询未删除的普通用户池（管理操作的通用前置）。 */
    public  Optional<UserPoolEntity> lockOrdinaryUserPool(UUID userPoolId) {
        return sql.createQuery(USER_POOL).where(USER_POOL.id().eq(userPoolId), USER_POOL.system().eq(false), USER_POOL.status().ne("deleted")).select(USER_POOL.fetch(UserPoolEntityFetcher.$.allScalarFields().packageInfo(UserPoolPackageEntityFetcher.$.allScalarFields()))).forUpdate().fetchOptional();
    }

    /** 按 ID 查询普通用户池（不加锁）。 */
    public Optional<UserPoolEntity> findOrdinaryUserPool(UUID userPoolId) {
        return sql.createQuery(USER_POOL).where(USER_POOL.id().eq(userPoolId), USER_POOL.system().eq(false)).select(USER_POOL.fetch(UserPoolEntityFetcher.$.allScalarFields().packageInfo(UserPoolPackageEntityFetcher.$.allScalarFields()))).fetchOptional();
    }

    /** 查询用户池当前的激活管理员成员关系（role=tenant_admin 且 status=active）。 */
    public Optional<UserPoolMembershipEntity> activeAdministratorMembership(UUID userPoolId) {
        return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.userPoolId().eq(userPoolId), MEMBERSHIP.membershipRole().eq("tenant_admin"), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).fetchOptional();
    }

    /** 加行级锁查询用户池当前的激活管理员成员关系。 */
    public Optional<UserPoolMembershipEntity> lockActiveAdministratorMembership(UUID userPoolId) {
        return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.userPoolId().eq(userPoolId), MEMBERSHIP.membershipRole().eq("tenant_admin"), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).forUpdate().fetchOptional();
    }

    /** 加行级锁查询某账号在指定用户池的成员关系。 */
    public Optional<UserPoolMembershipEntity> lockMembership(UUID userPoolId, UUID accountId) {
        return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.userPoolId().eq(userPoolId), MEMBERSHIP.accountId().eq(accountId)).select(MEMBERSHIP).forUpdate().fetchOptional();
    }

    /** 创建普通用户池（status=active，system=false）并绑定套餐，返回新实体。 */
    public UserPoolEntity createOrdinaryUserPool(String name, long packageId) {
        Instant now = Instant.now();
        var tenant = UserPoolEntityDraft.$.produce(draft -> draft.setId(UuidV7.randomUuid()).setName(name).setStatus("active").setSystem(false).setPackageInfo(UserPoolPackageEntityDraft.$.produce(userPoolPackage -> userPoolPackage.setId(packageId))).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(tenant).setMode(SaveMode.INSERT_ONLY).execute();
        return tenant;
    }

    /** 创建系统用户池（不绑定普通用户池套餐）。 */
    public UserPoolEntity createSystemUserPool(String name) {
        Instant now = Instant.now();
        UserPoolEntity tenant = UserPoolEntityDraft.$.produce(draft -> draft
                .setId(UuidV7.randomUuid())
                .setName(name)
                .setStatus("active")
                .setSystem(true)
                .setPackageInfo(null)
                .setCreatedAt(now)
                .setUpdatedAt(now));
        sql.saveCommand(tenant)
                .setMode(SaveMode.INSERT_ONLY)
                .execute();
        return tenant;
    }

    /** 更新普通用户池的名称与绑定套餐。 */
    public void updateOrdinaryUserPool(UUID userPoolId, String name, long packageId) {
        var tenant = UserPoolEntityDraft.$.produce(draft -> draft.setId(userPoolId).setName(name).setPackageInfo(UserPoolPackageEntityDraft.$.produce(userPoolPackage -> userPoolPackage.setId(packageId))).setUpdatedAt(Instant.now()));
        sql.saveCommand(tenant).setMode(SaveMode.UPDATE_ONLY).execute();
    }

    /** 更新普通用户池状态（active / suspended / deleted）。 */
    public void updateUserPoolStatus(UUID userPoolId, String status) {
        sql.createUpdate(USER_POOL).set(USER_POOL.status(), status).set(USER_POOL.updatedAt(), Instant.now()).where(USER_POOL.id().eq(userPoolId), USER_POOL.system().eq(false)).execute();
    }

    /** 创建一条激活状态的用户池成员关系（指定角色）。 */
    public void createActiveAdministratorMembership(UUID userPoolId, UUID accountId, String role) {
        Instant now = Instant.now();
        sql.saveCommand(UserPoolMembershipEntityDraft.$.produce(draft -> draft.setId(UuidV7.randomUuid()).setUserPoolId(userPoolId).setAccountId(accountId).setMembershipRole(role).setStatus("active").setCreatedAt(now).setUpdatedAt(now))).setMode(SaveMode.INSERT_ONLY).execute();
    }

    /** 停用（suspended）指定成员关系。 */
    public void suspendMembership(UUID membershipId) {
        sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.status(), "suspended").set(MEMBERSHIP.updatedAt(), Instant.now()).where(MEMBERSHIP.id().eq(membershipId), MEMBERSHIP.status().eq("active")).execute();
    }

    /** 停用某用户池下全部激活的成员关系（用于用户池删除）。 */
    public void suspendAllMemberships(UUID userPoolId) {
        sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.status(), "suspended").set(MEMBERSHIP.updatedAt(), Instant.now()).where(MEMBERSHIP.userPoolId().eq(userPoolId), MEMBERSHIP.status().eq("active")).execute();
    }

    /** 重新激活被停用的管理员成员关系并更新角色。 */
    public void activateAdministratorMembership(UUID membershipId, String role) {
        sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.membershipRole(), role).set(MEMBERSHIP.status(), "active").set(MEMBERSHIP.updatedAt(), Instant.now()).where(MEMBERSHIP.id().eq(membershipId), MEMBERSHIP.status().eq("suspended")).execute();
    }

    /** 提升激活成员关系的角色（普通成员升为管理员）。 */
    public void promoteActiveMembership(UUID membershipId, String role) {
        sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.membershipRole(), role).set(MEMBERSHIP.updatedAt(), Instant.now()).where(MEMBERSHIP.id().eq(membershipId), MEMBERSHIP.status().eq("active")).execute();
    }

    /** 加锁查询激活用户池套餐允许的最大用户数，套餐不可用时抛出配额异常。 */
    public int lockAndGetMaxUsers(UUID userPoolId) {
        return sql.createQuery(USER_POOL)
                .where(USER_POOL.id().eq(userPoolId), USER_POOL.status().eq("active"), USER_POOL.system().eq(false), USER_POOL.packageInfo().status().eq("active"))
                .select(USER_POOL.packageInfo().maxUsers())
                .forUpdate()
                .fetchOptional()
                .orElseThrow(() -> quotaUnavailable(ErrorCodeConstants.USER_POOL_QUOTA_USERS_UNAVAILABLE));
    }

    /** 加锁查询激活用户池套餐允许的最大应用数，套餐不可用时抛出配额异常。 */
    public int lockAndGetMaxApps(UUID userPoolId) {
        return sql.createQuery(USER_POOL)
                .where(USER_POOL.id().eq(userPoolId), USER_POOL.status().eq("active"), USER_POOL.system().eq(false), USER_POOL.packageInfo().status().eq("active"))
                .select(USER_POOL.packageInfo().maxApps())
                .forUpdate()
                .fetchOptional()
                .orElseThrow(() -> quotaUnavailable(ErrorCodeConstants.USER_POOL_QUOTA_APPLICATIONS_UNAVAILABLE));
    }

    /** 将配额错误码包装为领域异常。 */
    private static DomainException quotaUnavailable(ErrorCode errorCode) {
        return new DomainException(errorCode);
    }

}
