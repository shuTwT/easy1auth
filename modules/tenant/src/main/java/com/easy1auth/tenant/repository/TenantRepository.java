package com.easy1auth.tenant.repository;

import com.easy1auth.infrastructure.foundation.id.UuidV7;
import com.easy1auth.infrastructure.foundation.error.ErrorCode;
import com.easy1auth.tenant.ErrorCodeConstants;
import com.easy1auth.tenant.model.*;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

/**
 * 租户数据访问仓储（包私有）。
 *
 * <p>封装对 tenant / tenant_membership / tenant_package 表的所有读写，
 * 通过 jimmer {@link JSqlClient} 完成查询与更新。需要并发安全的地方使用
 * {@code forUpdate} 行级锁，保证配额与管理员身份在并发下的一致性。</p>
 */
@Repository
public class TenantRepository {
    /** tenant 表静态描述符 */
    private static final TenantEntityTable TENANT = TenantEntityTable.$;
    /** tenant_membership 表静态描述符 */
    private static final TenantMembershipEntityTable MEMBERSHIP = TenantMembershipEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;

    public TenantRepository(JSqlClient sql) {
        this.sql = sql;
    }

    /** 查询账号名下处于 active 状态的租户（含成员角色），按创建时间倒序。 */
    public List<TenantState> listForAccount(UUID accountId) {
        var memberships = sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(accountId), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).execute();
        if (memberships.isEmpty()) {
            return List.of();
        }
        var byTenant = new HashMap<UUID, String>();
        memberships.forEach(m -> byTenant.put(m.tenantId(), m.membershipRole()));
        return sql.createQuery(TENANT).where(TENANT.id().in(byTenant.keySet())).orderBy(TENANT.createdAt().desc()).select(TENANT.fetch(TenantEntityFetcher.$.allScalarFields().packageInfo(TenantPackageEntityFetcher.$.allScalarFields()))).execute().stream().map(t -> new TenantState(t, byTenant.get(t.id()))).toList();
    }

    /** 查询全部普通租户（非系统租户），按创建时间倒序。 */
    public List<TenantEntity> listOrdinaryTenants() {
        return sql.createQuery(TENANT).where(TENANT.system().eq(false)).orderBy(TENANT.createdAt().desc()).select(TENANT.fetch(TenantEntityFetcher.$.allScalarFields().packageInfo(TenantPackageEntityFetcher.$.allScalarFields()))).execute();
    }

    /** 查询全部租户（含系统租户），按创建时间倒序。 */
    public List<TenantEntity> listAllTenants() {
        return sql.createQuery(TENANT).orderBy(TENANT.createdAt().desc()).select(TENANT.fetch(TenantEntityFetcher.$.allScalarFields().packageInfo(TenantPackageEntityFetcher.$.allScalarFields()))).execute();
    }

    /** 查询账号在某租户的有效成员关系（含租户的套餐与系统标记）；租户非 active 时返回空。 */
    public Optional<MembershipState> activeMembership(UUID accountId, UUID tenantId) {
        var membership = sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(accountId), MEMBERSHIP.tenantId().eq(tenantId), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).fetchOptional();
        if (membership.isEmpty()) {
            return Optional.empty();
        }
        var tenant = sql.createQuery(TENANT).where(TENANT.id().eq(tenantId), TENANT.status().eq("active")).select(TENANT.fetch(TenantEntityFetcher.$.allScalarFields().packageInfo(TenantPackageEntityFetcher.$.allScalarFields()))).fetchOptional();
        if (tenant.isEmpty()) {
            return Optional.empty();
        }
        var tenantPackage = tenant.get().packageInfo();
        return Optional.of(new MembershipState(membership.get().id(), membership.get().membershipRole(), tenant.get().isSystem(), tenantPackage == null ? null : tenantPackage.id()));
    }

    /** 按 ID 加行级锁查询激活中的租户（用于安全材料生成前的并发保护）。 */
    public Optional<TenantEntity> lockActiveTenant(UUID tenantId) {
        return sql.createQuery(TENANT).where(TENANT.id().eq(tenantId), TENANT.status().eq("active")).select(TENANT).forUpdate().fetchOptional();
    }

    /** 按 ID 加行级锁查询未删除的普通租户（管理操作的通用前置）。 */
    public  Optional<TenantEntity> lockOrdinaryTenant(UUID tenantId) {
        return sql.createQuery(TENANT).where(TENANT.id().eq(tenantId), TENANT.system().eq(false), TENANT.status().ne("deleted")).select(TENANT.fetch(TenantEntityFetcher.$.allScalarFields().packageInfo(TenantPackageEntityFetcher.$.allScalarFields()))).forUpdate().fetchOptional();
    }

    /** 按 ID 查询普通租户（不加锁）。 */
    public Optional<TenantEntity> findOrdinaryTenant(UUID tenantId) {
        return sql.createQuery(TENANT).where(TENANT.id().eq(tenantId), TENANT.system().eq(false)).select(TENANT.fetch(TenantEntityFetcher.$.allScalarFields().packageInfo(TenantPackageEntityFetcher.$.allScalarFields()))).fetchOptional();
    }

    /** 查询租户当前的激活管理员成员关系（role=tenant_admin 且 status=active）。 */
    public Optional<TenantMembershipEntity> activeAdministratorMembership(UUID tenantId) {
        return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.tenantId().eq(tenantId), MEMBERSHIP.membershipRole().eq("tenant_admin"), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).fetchOptional();
    }

    /** 加行级锁查询租户当前的激活管理员成员关系。 */
    public Optional<TenantMembershipEntity> lockActiveAdministratorMembership(UUID tenantId) {
        return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.tenantId().eq(tenantId), MEMBERSHIP.membershipRole().eq("tenant_admin"), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).forUpdate().fetchOptional();
    }

    /** 加行级锁查询某账号在指定租户的成员关系。 */
    public Optional<TenantMembershipEntity> lockMembership(UUID tenantId, UUID accountId) {
        return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.tenantId().eq(tenantId), MEMBERSHIP.accountId().eq(accountId)).select(MEMBERSHIP).forUpdate().fetchOptional();
    }

    /** 创建普通租户（status=active，system=false）并绑定套餐，返回新实体。 */
    public TenantEntity createOrdinaryTenant(String name, long packageId) {
        Instant now = Instant.now();
        var tenant = TenantEntityDraft.$.produce(draft -> draft.setId(UuidV7.randomUuid()).setName(name).setStatus("active").setSystem(false).setPackageInfo(TenantPackageEntityDraft.$.produce(tenantPackage -> tenantPackage.setId(packageId))).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(tenant).setMode(SaveMode.INSERT_ONLY).execute();
        return tenant;
    }

    /** 更新普通租户的名称与绑定套餐。 */
    public void updateOrdinaryTenant(UUID tenantId, String name, long packageId) {
        var tenant = TenantEntityDraft.$.produce(draft -> draft.setId(tenantId).setName(name).setPackageInfo(TenantPackageEntityDraft.$.produce(tenantPackage -> tenantPackage.setId(packageId))).setUpdatedAt(Instant.now()));
        sql.saveCommand(tenant).setMode(SaveMode.UPDATE_ONLY).execute();
    }

    /** 更新普通租户状态（active / suspended / deleted）。 */
    public void updateTenantStatus(UUID tenantId, String status) {
        sql.createUpdate(TENANT).set(TENANT.status(), status).set(TENANT.updatedAt(), Instant.now()).where(TENANT.id().eq(tenantId), TENANT.system().eq(false)).execute();
    }

    /** 创建一条激活状态的租户成员关系（指定角色）。 */
    public void createActiveAdministratorMembership(UUID tenantId, UUID accountId, String role) {
        Instant now = Instant.now();
        sql.saveCommand(TenantMembershipEntityDraft.$.produce(draft -> draft.setId(UuidV7.randomUuid()).setTenantId(tenantId).setAccountId(accountId).setMembershipRole(role).setStatus("active").setCreatedAt(now).setUpdatedAt(now))).setMode(SaveMode.INSERT_ONLY).execute();
    }

    /** 停用（suspended）指定成员关系。 */
    public void suspendMembership(UUID membershipId) {
        sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.status(), "suspended").set(MEMBERSHIP.updatedAt(), Instant.now()).where(MEMBERSHIP.id().eq(membershipId), MEMBERSHIP.status().eq("active")).execute();
    }

    /** 停用某租户下全部激活的成员关系（用于租户删除）。 */
    public void suspendAllMemberships(UUID tenantId) {
        sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.status(), "suspended").set(MEMBERSHIP.updatedAt(), Instant.now()).where(MEMBERSHIP.tenantId().eq(tenantId), MEMBERSHIP.status().eq("active")).execute();
    }

    /** 重新激活被停用的管理员成员关系并更新角色。 */
    public void activateAdministratorMembership(UUID membershipId, String role) {
        sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.membershipRole(), role).set(MEMBERSHIP.status(), "active").set(MEMBERSHIP.updatedAt(), Instant.now()).where(MEMBERSHIP.id().eq(membershipId), MEMBERSHIP.status().eq("suspended")).execute();
    }

    /** 提升激活成员关系的角色（普通成员升为管理员）。 */
    public void promoteActiveMembership(UUID membershipId, String role) {
        sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.membershipRole(), role).set(MEMBERSHIP.updatedAt(), Instant.now()).where(MEMBERSHIP.id().eq(membershipId), MEMBERSHIP.status().eq("active")).execute();
    }

    /** 加锁查询激活租户套餐允许的最大用户数，套餐不可用时抛出配额异常。 */
    public int lockAndGetMaxUsers(UUID tenantId) {
        return sql.createQuery(TENANT)
                .where(TENANT.id().eq(tenantId), TENANT.status().eq("active"), TENANT.system().eq(false), TENANT.packageInfo().status().eq("active"))
                .select(TENANT.packageInfo().maxUsers())
                .forUpdate()
                .fetchOptional()
                .orElseThrow(() -> quotaUnavailable(ErrorCodeConstants.TENANT_QUOTA_USERS_UNAVAILABLE));
    }

    /** 加锁查询激活租户套餐允许的最大应用数，套餐不可用时抛出配额异常。 */
    public int lockAndGetMaxApps(UUID tenantId) {
        return sql.createQuery(TENANT)
                .where(TENANT.id().eq(tenantId), TENANT.status().eq("active"), TENANT.system().eq(false), TENANT.packageInfo().status().eq("active"))
                .select(TENANT.packageInfo().maxApps())
                .forUpdate()
                .fetchOptional()
                .orElseThrow(() -> quotaUnavailable(ErrorCodeConstants.TENANT_QUOTA_APPLICATIONS_UNAVAILABLE));
    }

    /** 将配额错误码包装为领域异常。 */
    private static DomainException quotaUnavailable(ErrorCode errorCode) {
        return new DomainException(errorCode);
    }

}
