package com.easy1auth.tenant.service;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.security.ActiveAdminAccountLocker;
import com.easy1auth.tenant.*;
import com.easy1auth.tenant.constant.ErrorCodeConstants;
import com.easy1auth.tenant.dto.TenantControlView;
import com.easy1auth.tenant.dto.TenantPackageView;
import com.easy1auth.tenant.dto.TenantSummary;
import com.easy1auth.tenant.model.TenantEntity;
import com.easy1auth.tenant.infrastructure.repository.TenantRepository;
import com.easy1auth.tenant.dto.TenantState;
import com.easy1auth.tenant.dto.TenantAuthorizationRequest;
import com.easy1auth.tenant.util.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 租户服务：租户生命周期管理（创建、更新、状态变更、删除、管理员转移），
 * 以及租户上下文解析、可用套餐查询与配额锁定。
 *
 * <p>所有变更操作均处于事务边界内，并在执行前完成账号与套餐的并发锁定，
 * 以保证多租户配额与管理员身份的一致性。</p>
 */
@Service
public class TenantService {
    /** 租户数据访问仓储 */
    private final TenantRepository repository;
    /** 租户访问授权解析器（解析权限与套餐） */
    private final TenantAuthorizationProvider authorization;
    /** 租户套餐服务 */
    private final TenantPackageService packages;
    /** 管理账号锁定器（防止并发操作正在被变更的账号） */
    private final ActiveAdminAccountLocker administratorAccounts;

    TenantService(TenantRepository repository, TenantAuthorizationProvider authorization, TenantPackageService packages,
                  ActiveAdminAccountLocker administratorAccounts) {
        this.repository = repository;
        this.authorization = authorization;
        this.packages = packages;
        this.administratorAccounts = administratorAccounts;
    }

    /** 查询指定管理账号可访问的租户摘要列表（含账号在各租户中的角色）。 */
    @Transactional(readOnly = true)
    public List<TenantSummary> list(UUID accountId) {
        return repository.listForAccount(accountId).stream().map(this::summary).toList();
    }

    /** 查询全部租户摘要（平台管理视角，标注每个租户的默认角色）。 */
    @Transactional(readOnly = true)
    public List<TenantSummary> listAll() {
        return repository.listAllTenants().stream().map(this::managementSummary).toList();
    }

    /** 查询全部普通租户的控制视图（用于平台侧租户管理列表）。 */
    @Transactional(readOnly = true)
    public List<TenantControlView> listManaged() {
        return repository.listOrdinaryTenants().stream().map(this::controlView).toList();
    }

    /** 查询可分配给普通租户的启用中套餐列表。 */
    @Transactional(readOnly = true)
    public List<TenantPackageView> listAssignablePackages() {
        return packages.list().stream()
                .filter(item -> "active".equals(item.status()))
                .toList();
    }

    /**
     * 解析租户上下文：校验账号在该租户的有效成员关系与角色，
     * 并解析出该租户下的权限集合与套餐信息。
     *
     * @param traceId 链路追踪 ID（透传，用于日志串联）
     * @return 租户上下文，供后续请求的权限判断使用
     */
    @Transactional(readOnly = true)
    public TenantContext resolve(UUID accountId, UUID tenantId, String traceId) {
        var membership = repository.activeMembership(accountId, tenantId).filter(it -> authorization.isActiveAccount(accountId)).orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_ACCESS_DENIED));
        validateMembershipRole(membership.system(), membership.role());
        var effective = authorization.resolve(new TenantAuthorizationRequest(
                accountId, tenantId, membership.id(), membership.role(), membership.system(), membership.packageId()));
        return new TenantContext(
                accountId, tenantId, membership.id(), membership.role(), effective.permissions(),
                effective.tenantPackage(), traceId);
    }

    /** 创建普通租户并绑定指定套餐，同时将指定管理账号设为租户管理员。 */
    @Transactional
    public TenantSummary createOrdinary(String name, long packageId, UUID administratorAccountId) {
        var tenantPackage = packages.lockActiveAssignable(packageId);
        administratorAccounts.lockActive(administratorAccountId);
        return createOrdinary(name, tenantPackage, administratorAccountId);
    }

    /** 创建普通租户并绑定默认可用套餐，同时将指定管理账号设为租户管理员。 */
    @Transactional
    public TenantSummary createOrdinaryWithDefaultPackage(String name, UUID administratorAccountId) {
        var tenantPackage = packages.lockActiveDefaultAssignable();
        administratorAccounts.lockActive(administratorAccountId);
        return createOrdinary(name, tenantPackage, administratorAccountId);
    }

    /** 更新普通租户名称或绑定的套餐（仅更新传入的非空字段）。 */
    @Transactional
    public TenantControlView updateOrdinary(UUID tenantId, String name, Long packageId) {
        TenantEntity existing = requiredOrdinaryTenant(tenantId);
        String normalizedName = name == null ? existing.name() : normalizeName(name);
        long assignedPackageId = packageId == null
                ? Objects.requireNonNull(existing.packageInfo(), "ordinary tenant package missing").id()
                : packages.lockActiveAssignable(packageId).id();
        repository.updateOrdinaryTenant(existing.id(), normalizedName, assignedPackageId);
        return controlView(requiredOrdinaryTenant(existing.id()));
    }

    /** 更新普通租户状态（active / suspended）；恢复为 active 时校验套餐仍可分配。 */
    @Transactional
    public TenantControlView updateOrdinaryStatus(UUID tenantId, String status) {
        TenantEntity tenant = requiredOrdinaryTenant(tenantId);
        String normalized = status == null ? "" : status.strip();
        if (!"active".equals(normalized) && !"suspended".equals(normalized)) {
            throw new DomainException(ErrorCodeConstants.TENANT_STATUS_INVALID);
        }
        if ("active".equals(normalized)) {
            packages.lockActiveAssignable(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing").id());
        }
        repository.updateTenantStatus(tenant.id(), normalized);
        return controlView(requiredOrdinaryTenant(tenant.id()));
    }

    /** 删除普通租户（软删除）：置为 deleted 状态并停用其全部成员关系。 */
    @Transactional
    public void deleteOrdinary(UUID tenantId) {
        TenantEntity tenant = requiredOrdinaryTenant(tenantId);
        repository.updateTenantStatus(tenant.id(), "deleted");
        repository.suspendAllMemberships(tenant.id());
    }

    /** 转移租户管理员：将当前管理员降为普通成员，并将目标账号提升为租户管理员。 */
    @Transactional
    public TenantControlView transferAdministrator(UUID tenantId, UUID targetAccountId) {
        if (targetAccountId == null) {
            throw new DomainException(ErrorCodeConstants.ADMINISTRATOR_ACCOUNT_REQUIRED);
        }
        TenantEntity tenant = requiredOrdinaryTenant(tenantId);
        if (!"active".equals(tenant.status())) {
            throw new DomainException(ErrorCodeConstants.TENANT_NOT_ACTIVE);
        }
        String administratorRole = administratorRole(false);
        administratorAccounts.lockActive(targetAccountId);
        var current = repository.lockActiveAdministratorMembership(tenant.id())
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_ADMINISTRATOR_MISSING));
        if (current.accountId().equals(targetAccountId)) {
            throw new DomainException(ErrorCodeConstants.TENANT_ADMINISTRATOR_TARGET_CURRENT);
        }
        var targetMembership = repository.lockMembership(tenant.id(), targetAccountId);
        if (targetMembership.isPresent()) {
            if ("active".equals(targetMembership.get().status())) {
                repository.promoteActiveMembership(targetMembership.get().id(), administratorRole);
            } else {
                repository.activateAdministratorMembership(targetMembership.get().id(), administratorRole);
            }
        } else {
            repository.createActiveAdministratorMembership(tenant.id(), targetAccountId, administratorRole);
        }
        repository.suspendMembership(current.id());
        return controlView(requiredOrdinaryTenant(tenant.id()));
    }

    /** 锁定租户并返回其套餐允许的最大用户数（创建用户前的配额校验）。 */
    @Transactional
    public int lockForUserQuota(UUID tenantId) {
        return repository.lockAndGetMaxUsers(tenantId);
    }

    /** 锁定租户并返回其套餐允许的最大应用数（创建应用前的配额校验）。 */
    @Transactional
    public int lockForAppQuota(UUID tenantId) {
        return repository.lockAndGetMaxApps(tenantId);
    }

    /** 锁定激活中的租户，用于生成安全材料（如签名密钥）前的并发保护。 */
    @Transactional
    public void lockForSecurityMaterial(UUID tenantId) {
        repository.lockActiveTenant(tenantId).orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_NOT_FOUND_ACTIVE));
    }

    /** 将仓储返回的租户状态转换为账号视角的租户摘要。 */
    private TenantSummary summary(TenantState state) {
        var tenant = state.tenant();
        validateMembershipRole(tenant.isSystem(), state.role());
        var tenantPackage = tenant.isSystem()
                ? packages.systemPackage()
                : packages.view(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing"));
        return new TenantSummary(tenant.id(), tenant.name(), tenant.status(), tenant.isSystem(), tenantPackage, state.role());
    }

    /** 将租户实体转换为平台管理视角的租户摘要（标注默认角色）。 */
    private TenantSummary managementSummary(TenantEntity tenant) {
        var tenantPackage = tenant.isSystem()
                ? packages.systemPackage()
                : packages.view(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing"));
        return new TenantSummary(tenant.id(), tenant.name(), tenant.status(), tenant.isSystem(), tenantPackage,
                tenant.isSystem() ? "super_admin" : "tenant_admin");
    }

    /** 将租户实体转换为控制视图（含当前管理员账号 ID）。 */
    private TenantControlView controlView(TenantEntity tenant) {
        var tenantPackage = packages.view(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing"));
        UUID administratorAccountId = repository.activeAdministratorMembership(tenant.id())
                .map(membership -> membership.accountId())
                .orElse(null);
        return new TenantControlView(tenant.id(), tenant.name(), tenant.status(), tenantPackage, administratorAccountId);
    }

    /** 按 ID 锁定并返回普通租户，不存在或已被删除时抛出领域异常。 */
    private TenantEntity requiredOrdinaryTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new DomainException(ErrorCodeConstants.TENANT_ID_REQUIRED);
        }
        return repository.lockOrdinaryTenant(tenantId)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_NOT_FOUND_ORDINARY));
    }

    /** 创建普通租户的内部实现（调用方须先完成套餐与账号锁定）。 */
    private TenantSummary createOrdinary(String name, TenantPackageView tenantPackage, UUID administratorAccountId) {
        String normalizedName = normalizeName(name);
        TenantEntity tenant = repository.createOrdinaryTenant(normalizedName, tenantPackage.id());
        repository.createActiveAdministratorMembership(tenant.id(), administratorAccountId, administratorRole(false));
        return new TenantSummary(tenant.id(), tenant.name(), tenant.status(), false, tenantPackage, administratorRole(false));
    }

    /** 返回指定类型租户的管理员角色名（系统租户 super_admin，普通租户 tenant_admin）。 */
    private static String administratorRole(boolean systemTenant) {
        return systemTenant ? "super_admin" : "tenant_admin";
    }

    /** 校验成员角色与租户类型是否匹配，不匹配时抛出领域异常。 */
    private static void validateMembershipRole(boolean systemTenant, String membershipRole) {
        if ("common".equals(membershipRole)) {
            return;
        }
        if ((systemTenant && "super_admin".equals(membershipRole))
                || (!systemTenant && "tenant_admin".equals(membershipRole))) {
            return;
        }
        throw new DomainException(ErrorCodeConstants.TENANT_MEMBERSHIP_ROLE_INVALID);
    }

    /** 规范化租户名称：去除首尾空白并校验长度（1-200）。 */
    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.strip();
        if (normalized.isEmpty() || normalized.length() > 200) {
            throw new DomainException(ErrorCodeConstants.TENANT_NAME_INVALID);
        }
        return normalized;
    }
}
