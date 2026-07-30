package com.easy1auth.tenant;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.security.ActiveAdminAccountLocker;
import com.easy1auth.tenant.model.TenantEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TenantService {
    private final TenantRepository repository;
    private final TenantAuthorizationProvider authorization;
    private final TenantPackageService packages;
    private final ActiveAdminAccountLocker administratorAccounts;
    TenantService(TenantRepository repository, TenantAuthorizationProvider authorization, TenantPackageService packages,
                  ActiveAdminAccountLocker administratorAccounts) { this.repository = repository; this.authorization = authorization; this.packages = packages; this.administratorAccounts = administratorAccounts; }

    @Transactional(readOnly = true) public List<TenantSummary> list(UUID accountId) { return repository.listForAccount(accountId).stream().map(this::summary).toList(); }
    @Transactional(readOnly = true)
    public TenantContext resolve(UUID accountId, UUID tenantId, String traceId) {
        var membership=repository.activeMembership(accountId,tenantId).filter(it->authorization.isActiveAccount(accountId)).orElseThrow(() -> new DomainException("TENANT_ACCESS_DENIED", "无权访问所选租户", 403));
        validateMembershipRole(membership.system(), membership.role());
        var effective = authorization.resolve(new TenantAuthorizationRequest(
                accountId, tenantId, membership.id(), membership.role(), membership.system(), membership.packageId()));
        return new TenantContext(
                accountId, tenantId, membership.id(), membership.role(), effective.permissions(),
                effective.dataBoundary(), effective.tenantPackage(), traceId);
    }
    @Transactional
    public TenantSummary createOrdinary(String name, long packageId, UUID administratorAccountId) {
        var tenantPackage = packages.lockActiveAssignable(packageId);
        administratorAccounts.lockActive(administratorAccountId);
        return createOrdinary(name, tenantPackage, administratorAccountId);
    }
    @Transactional
    public TenantSummary createOrdinaryWithDefaultPackage(String name, UUID administratorAccountId) {
        var tenantPackage = packages.lockActiveDefaultAssignable();
        administratorAccounts.lockActive(administratorAccountId);
        return createOrdinary(name, tenantPackage, administratorAccountId);
    }
    @Transactional
    public void transferAdministrator(UUID serverResolvedTenantId, UUID targetAccountId) {
        if (targetAccountId == null) {
            throw new DomainException("ADMINISTRATOR_ACCOUNT_REQUIRED", "管理员账号不能为空", 400);
        }
        TenantEntity tenant = repository.lockActiveTenant(serverResolvedTenantId)
                .orElseThrow(() -> new DomainException("TENANT_NOT_FOUND", "租户不存在或未启用", 404));
        String administratorRole = administratorRole(tenant.isSystem());
        administratorAccounts.lockActive(targetAccountId);
        var current = repository.lockActiveMembership(tenant.id())
                .orElseThrow(() -> new DomainException("TENANT_ADMINISTRATOR_MISSING", "租户缺少有效管理员", 409));
        if (!administratorRole.equals(current.membershipRole())) {
            throw new DomainException("TENANT_ADMINISTRATOR_ROLE_INVALID", "租户管理员角色与租户类型不匹配", 409);
        }
        if (current.accountId().equals(targetAccountId)) {
            throw new DomainException("TENANT_ADMINISTRATOR_TARGET_CURRENT", "目标账号已是当前租户管理员", 409);
        }
        var targetMembership = repository.lockMembership(tenant.id(), targetAccountId);
        if (targetMembership.isPresent() && "active".equals(targetMembership.get().status())) {
            throw new DomainException("TENANT_ADMINISTRATOR_TARGET_ACTIVE", "目标账号已拥有有效租户成员关系", 409);
        }
        repository.suspendMembership(current.id());
        if (targetMembership.isPresent()) {
            repository.activateAdministratorMembership(targetMembership.get().id(), administratorRole);
        } else {
            repository.createActiveAdministratorMembership(tenant.id(), targetAccountId, administratorRole);
        }
    }
    @Transactional public int lockForUserQuota(UUID tenantId){return repository.lockAndGetMaxUsers(tenantId);}
    @Transactional public int lockForAppQuota(UUID tenantId){return repository.lockAndGetMaxApps(tenantId);}
    @Transactional public void lockForSecurityMaterial(UUID tenantId){repository.lockActiveTenant(tenantId).orElseThrow(() -> new DomainException("TENANT_NOT_FOUND", "租户不存在或未启用", 404));}

    private TenantSummary summary(TenantRepository.TenantState state) {
        var tenant = state.tenant();
        validateMembershipRole(tenant.isSystem(), state.role());
        var tenantPackage = tenant.isSystem()
                ? packages.systemPackage()
                : packages.view(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing"));
        return new TenantSummary(tenant.id(), tenant.name(), tenant.status(), tenant.isSystem(), tenantPackage, state.role());
    }

    private TenantSummary createOrdinary(String name, TenantPackageView tenantPackage, UUID administratorAccountId) {
        String normalizedName = normalizeName(name);
        TenantEntity tenant = repository.createOrdinaryTenant(normalizedName, tenantPackage.id());
        repository.createActiveAdministratorMembership(tenant.id(), administratorAccountId, administratorRole(false));
        return new TenantSummary(tenant.id(), tenant.name(), tenant.status(), false, tenantPackage, administratorRole(false));
    }

    private static String administratorRole(boolean systemTenant) {
        return systemTenant ? "super_admin" : "tenant_admin";
    }

    private static void validateMembershipRole(boolean systemTenant, String membershipRole) {
        if ("common".equals(membershipRole)) {
            return;
        }
        if ((systemTenant && "super_admin".equals(membershipRole))
                || (!systemTenant && "tenant_admin".equals(membershipRole))) {
            return;
        }
        throw new DomainException("TENANT_MEMBERSHIP_ROLE_INVALID", "成员角色与租户类型不匹配", 409);
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.strip();
        if (normalized.isEmpty() || normalized.length() > 200) {
            throw new DomainException("TENANT_NAME_INVALID", "租户名称不能为空且不能超过200个字符", 400);
        }
        return normalized;
    }
}
