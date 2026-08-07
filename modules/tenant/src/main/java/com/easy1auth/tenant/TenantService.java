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
                  ActiveAdminAccountLocker administratorAccounts) {
        this.repository = repository;
        this.authorization = authorization;
        this.packages = packages;
        this.administratorAccounts = administratorAccounts;
    }

    @Transactional(readOnly = true)
    public List<TenantSummary> list(UUID accountId) {
        return repository.listForAccount(accountId).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<TenantSummary> listAll() {
        return repository.listAllTenants().stream().map(this::managementSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<TenantControlView> listManaged() {
        return repository.listOrdinaryTenants().stream().map(this::controlView).toList();
    }

    @Transactional(readOnly = true)
    public List<TenantPackageView> listAssignablePackages() {
        return packages.list().stream()
                .filter(item -> "active".equals(item.status()))
                .toList();
    }

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
    public TenantControlView updateOrdinary(UUID tenantId, String name, Long packageId) {
        TenantEntity existing = requiredOrdinaryTenant(tenantId);
        String normalizedName = name == null ? existing.name() : normalizeName(name);
        long assignedPackageId = packageId == null
                ? Objects.requireNonNull(existing.packageInfo(), "ordinary tenant package missing").id()
                : packages.lockActiveAssignable(packageId).id();
        repository.updateOrdinaryTenant(existing.id(), normalizedName, assignedPackageId);
        return controlView(requiredOrdinaryTenant(existing.id()));
    }

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

    @Transactional
    public void deleteOrdinary(UUID tenantId) {
        TenantEntity tenant = requiredOrdinaryTenant(tenantId);
        repository.updateTenantStatus(tenant.id(), "deleted");
        repository.suspendAllMemberships(tenant.id());
    }

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

    @Transactional
    public int lockForUserQuota(UUID tenantId) {
        return repository.lockAndGetMaxUsers(tenantId);
    }

    @Transactional
    public int lockForAppQuota(UUID tenantId) {
        return repository.lockAndGetMaxApps(tenantId);
    }

    @Transactional
    public void lockForSecurityMaterial(UUID tenantId) {
        repository.lockActiveTenant(tenantId).orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_NOT_FOUND_ACTIVE));
    }

    private TenantSummary summary(TenantRepository.TenantState state) {
        var tenant = state.tenant();
        validateMembershipRole(tenant.isSystem(), state.role());
        var tenantPackage = tenant.isSystem()
                ? packages.systemPackage()
                : packages.view(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing"));
        return new TenantSummary(tenant.id(), tenant.name(), tenant.status(), tenant.isSystem(), tenantPackage, state.role());
    }

    private TenantSummary managementSummary(TenantEntity tenant) {
        var tenantPackage = tenant.isSystem()
                ? packages.systemPackage()
                : packages.view(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing"));
        return new TenantSummary(tenant.id(), tenant.name(), tenant.status(), tenant.isSystem(), tenantPackage,
                tenant.isSystem() ? "super_admin" : "tenant_admin");
    }

    private TenantControlView controlView(TenantEntity tenant) {
        var tenantPackage = packages.view(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing"));
        UUID administratorAccountId = repository.activeAdministratorMembership(tenant.id())
                .map(membership -> membership.accountId())
                .orElse(null);
        return new TenantControlView(tenant.id(), tenant.name(), tenant.status(), tenantPackage, administratorAccountId);
    }

    private TenantEntity requiredOrdinaryTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new DomainException(ErrorCodeConstants.TENANT_ID_REQUIRED);
        }
        return repository.lockOrdinaryTenant(tenantId)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_NOT_FOUND_ORDINARY));
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
        throw new DomainException(ErrorCodeConstants.TENANT_MEMBERSHIP_ROLE_INVALID);
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.strip();
        if (normalized.isEmpty() || normalized.length() > 200) {
            throw new DomainException(ErrorCodeConstants.TENANT_NAME_INVALID);
        }
        return normalized;
    }
}
