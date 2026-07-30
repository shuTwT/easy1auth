package com.easy1auth.adminaccess;

import com.easy1auth.adminidentity.model.AdminAccountEntityTable;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.tenant.model.TenantEntity;
import com.easy1auth.tenant.model.TenantEntityTable;
import com.easy1auth.tenant.model.TenantMembershipEntityTable;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class PlatformAuthorizationResolver {
    private static final AdminAccountEntityTable ACCOUNT = AdminAccountEntityTable.$;
    private static final TenantEntityTable TENANT = TenantEntityTable.$;
    private static final TenantMembershipEntityTable MEMBERSHIP = TenantMembershipEntityTable.$;

    private final JSqlClient sql;
    private final ManagementPermissionCatalog catalog;

    public PlatformAuthorizationResolver(JSqlClient sql, ManagementPermissionCatalog catalog) {
        this.sql = sql;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public PlatformAuthorization resolve(UUID accountId) {
        if (!isActiveAccount(accountId)) {
            throw accessDenied();
        }
        TenantEntity systemTenant = uniqueActiveSystemTenant();
        boolean activeSuperAdmin = sql.createQuery(MEMBERSHIP)
                .where(
                        MEMBERSHIP.accountId().eq(accountId),
                        MEMBERSHIP.tenantId().eq(systemTenant.id()),
                        MEMBERSHIP.membershipRole().eq("super_admin"),
                        MEMBERSHIP.status().eq("active"))
                .select(MEMBERSHIP.id())
                .exists();
        if (!activeSuperAdmin) {
            throw accessDenied();
        }
        Set<ManagementPermissionCode> permissions = Set.copyOf(catalog.activeCodes(ManagementPermissionScope.PLATFORM));
        if (!permissions.contains(ManagementPermissionCode.DATA_PLATFORM_ALL)) {
            throw new DomainException("PLATFORM_DATA_BOUNDARY_MISSING", "平台数据边界权限不可用", 409);
        }
        return new PlatformAuthorization(systemTenant.id(), permissions);
    }

    @Transactional(readOnly = true)
    public PlatformAuthorization require(UUID accountId, ManagementPermissionCode permission) {
        if (permission.scope() != ManagementPermissionScope.PLATFORM) {
            throw new DomainException("PLATFORM_PERMISSION_SCOPE_INVALID", "权限不属于平台作用域", 400);
        }
        PlatformAuthorization authorization = resolve(accountId);
        if (!authorization.has(permission)) {
            throw new DomainException("PERMISSION_DENIED", "权限不足", 403);
        }
        return authorization;
    }

    private boolean isActiveAccount(UUID accountId) {
        return sql.createQuery(ACCOUNT)
                .where(ACCOUNT.id().eq(accountId), ACCOUNT.status().eq("active"))
                .select(ACCOUNT.id())
                .exists();
    }

    private TenantEntity uniqueActiveSystemTenant() {
        var systemTenants = sql.createQuery(TENANT)
                .where(TENANT.system().eq(true))
                .select(TENANT)
                .execute();
        if (systemTenants.size() != 1 || !"active".equals(systemTenants.getFirst().status())) {
            throw new DomainException("SYSTEM_TENANT_INVALID", "系统租户状态无效", 409);
        }
        return systemTenants.getFirst();
    }

    private static DomainException accessDenied() {
        return new DomainException("PLATFORM_ACCESS_DENIED", "无权访问平台资源", 403);
    }
}
