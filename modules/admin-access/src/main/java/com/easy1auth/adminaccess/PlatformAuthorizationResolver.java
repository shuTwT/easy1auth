package com.easy1auth.adminaccess;

import com.easy1auth.adminaccess.constant.ErrorCodeConstants;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.adminaccess.constant.ManagementPermissionScope;
import com.easy1auth.adminaccess.dto.PlatformAuthorization;
import com.easy1auth.adminidentity.model.AdminAccountEntityTable;
import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.tenant.model.TenantEntity;
import com.easy1auth.tenant.model.TenantEntityTable;
import com.easy1auth.tenant.model.TenantMembershipEntityTable;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * 平台授权解析器：解析管理账号的平台级（PLATFORM 作用域）权限。
 *
 * <p>平台权限仅授予系统租户中处于 active 状态的 super_admin 账号。解析时依次
 * 校验账号有效性、系统租户唯一性以及账号是否为系统租户的激活超管，通过后
 * 返回其持有的全部平台权限码集合。</p>
 */
@Service
public class PlatformAuthorizationResolver {
    /** admin_account 表静态描述符 */
    private static final AdminAccountEntityTable ACCOUNT = AdminAccountEntityTable.$;
    /** tenant 表静态描述符 */
    private static final TenantEntityTable TENANT = TenantEntityTable.$;
    /** tenant_membership 表静态描述符 */
    private static final TenantMembershipEntityTable MEMBERSHIP = TenantMembershipEntityTable.$;

    /** jimmer SQL 客户端 */
    private final JSqlClient sql;
    /** 管理端权限目录（用于读取平台作用域的启用权限） */
    private final ManagementPermissionCatalog catalog;

    public PlatformAuthorizationResolver(JSqlClient sql, ManagementPermissionCatalog catalog) {
        this.sql = sql;
        this.catalog = catalog;
    }

    /** 解析账号的平台授权；账号非系统租户激活 super_admin 时抛出访问被拒异常。 */
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
        return new PlatformAuthorization(systemTenant.id(), permissions);
    }

    /** 校验账号持有指定平台权限：不满足则抛出权限不足异常，满足则返回平台授权。 */
    @Transactional(readOnly = true)
    public PlatformAuthorization require(UUID accountId, ManagementPermissionCode permission) {
        if (permission.scope() != ManagementPermissionScope.PLATFORM) {
            throw new DomainException(ErrorCodeConstants.PLATFORM_PERMISSION_SCOPE_INVALID);
        }
        PlatformAuthorization authorization = resolve(accountId);
        if (!authorization.has(permission)) {
            throw new DomainException(ErrorCodeConstants.PERMISSION_DENIED);
        }
        return authorization;
    }

    /** 判断账号是否为激活状态。 */
    private boolean isActiveAccount(UUID accountId) {
        return sql.createQuery(ACCOUNT)
                .where(ACCOUNT.id().eq(accountId), ACCOUNT.status().eq("active"))
                .select(ACCOUNT.id())
                .exists();
    }

    /** 查询系统中唯一且激活的系统租户，数量或状态异常时抛出领域异常。 */
    private TenantEntity uniqueActiveSystemTenant() {
        var systemTenants = sql.createQuery(TENANT)
                .where(TENANT.system().eq(true))
                .select(TENANT)
                .execute();
        if (systemTenants.size() != 1 || !"active".equals(systemTenants.getFirst().status())) {
            throw new DomainException(ErrorCodeConstants.SYSTEM_TENANT_INVALID);
        }
        return systemTenants.getFirst();
    }

    /** 构造平台访问被拒的领域异常。 */
    private static DomainException accessDenied() {
        return new DomainException(ErrorCodeConstants.PLATFORM_ACCESS_DENIED);
    }
}
