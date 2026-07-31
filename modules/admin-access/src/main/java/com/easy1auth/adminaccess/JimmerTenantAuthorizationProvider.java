package com.easy1auth.adminaccess;

import com.easy1auth.adminidentity.model.AdminAccountEntityTable;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.tenant.TenantAuthorization;
import com.easy1auth.tenant.TenantAuthorizationProvider;
import com.easy1auth.tenant.TenantAuthorizationRequest;
import com.easy1auth.tenant.TenantPackageService;
import com.easy1auth.tenant.TenantPackageView;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Component
final class JimmerTenantAuthorizationProvider implements TenantAuthorizationProvider {
    private static final AdminAccountEntityTable ACCOUNT = AdminAccountEntityTable.$;

    private final JSqlClient sql;
    private final ManagementPermissionCatalog catalog;
    private final TenantPackageService packages;
    private final PlatformAuthorizationResolver platforms;

    JimmerTenantAuthorizationProvider(
            JSqlClient sql,
            ManagementPermissionCatalog catalog,
            TenantPackageService packages,
            PlatformAuthorizationResolver platforms) {
        this.sql = sql;
        this.catalog = catalog;
        this.packages = packages;
        this.platforms = platforms;
    }

    @Override
    public boolean isActiveAccount(UUID accountId) {
        return sql.createQuery(ACCOUNT)
                .where(ACCOUNT.id().eq(accountId), ACCOUNT.status().eq("active"))
                .select(ACCOUNT.id())
                .exists();
    }

    @Override
    public TenantAuthorization resolve(TenantAuthorizationRequest request) {
        return request.systemTenant() ? resolveSystem(request) : resolveOrdinary(request);
    }

    private TenantAuthorization resolveSystem(TenantAuthorizationRequest request) {
        return switch (request.membershipRole()) {
            case "super_admin" -> resolveSuperAdmin(request);
            case "common" -> new TenantAuthorization(Set.of(), packages.systemPackage());
            default -> throw invalidMembershipRole();
        };
    }

    private TenantAuthorization resolveSuperAdmin(TenantAuthorizationRequest request) {
        var platform = platforms.resolve(request.accountId());
        if (!request.tenantId().equals(platform.tenantId())) {
            throw new DomainException("SYSTEM_TENANT_CONTEXT_INVALID", "系统租户上下文无效", 409);
        }
        Set<String> permissions = Stream.concat(
                        catalog.activeCodes(ManagementPermissionScope.PLATFORM).stream(),
                        catalog.activeCodes(ManagementPermissionScope.TENANT).stream())
                .map(ManagementPermissionCode::value)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new TenantAuthorization(permissions, packages.systemPackage());
    }

    private TenantAuthorization resolveOrdinary(TenantAuthorizationRequest request) {
        TenantPackageView tenantPackage = activeOrdinaryPackage(request);
        return switch (request.membershipRole()) {
            case "tenant_admin" -> new TenantAuthorization(packagePermissions(tenantPackage), tenantPackage);
            case "common" -> new TenantAuthorization(Set.of(), tenantPackage);
            default -> throw invalidMembershipRole();
        };
    }

    private TenantPackageView activeOrdinaryPackage(TenantAuthorizationRequest request) {
        if (request.packageId() == null || request.packageId() <= 0) {
            throw new DomainException("TENANT_PACKAGE_MISSING", "普通租户缺少有效套餐", 409);
        }
        return packages.getActive(request.packageId());
    }

    private Set<String> packagePermissions(TenantPackageView tenantPackage) {
        return catalog.validate(tenantPackage.permissionCodes(), ManagementPermissionScope.TENANT).stream()
                .map(ManagementPermissionCode::value)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static DomainException invalidMembershipRole() {
        return new DomainException("TENANT_MEMBERSHIP_ROLE_INVALID", "成员角色与租户类型不匹配", 409);
    }
}
