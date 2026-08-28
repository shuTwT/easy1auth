package com.easy1auth.adminaccess;

import com.easy1auth.adminidentity.model.AdminAccountEntityTable;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.tenant.TenantAuthorization;
import com.easy1auth.tenant.TenantAuthorizationProvider;
import com.easy1auth.tenant.TenantAuthorizationRequest;
import com.easy1auth.tenant.service.TenantPackageService;
import com.easy1auth.tenant.TenantPackageView;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 租户授权提供者的 jimmer 实现（包私有）。
 *
 * <p>基于 admin-access 域的权限目录解析租户授权：系统租户的 super_admin 合并
 * 平台与租户的全部启用权限；普通租户按套餐声明的权限码解析，tenant_admin 获得
 * 套餐权限，common 无任何权限。</p>
 */
@Component
final class JimmerTenantAuthorizationProvider implements TenantAuthorizationProvider {
    /** admin_account 表静态描述符 */
    private static final AdminAccountEntityTable ACCOUNT = AdminAccountEntityTable.$;

    /** jimmer SQL 客户端 */
    private final JSqlClient sql;
    /** 管理端权限目录 */
    private final ManagementPermissionCatalog catalog;
    /** 租户套餐服务 */
    private final TenantPackageService packages;
    /** 平台授权解析器（解析 super_admin 的平台权限） */
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

    /** 判断账号是否为激活状态（授权解析的前置校验）。 */
    @Override
    public boolean isActiveAccount(UUID accountId) {
        return sql.createQuery(ACCOUNT)
                .where(ACCOUNT.id().eq(accountId), ACCOUNT.status().eq("active"))
                .select(ACCOUNT.id())
                .exists();
    }

    /** 解析租户授权：系统租户与普通租户走不同逻辑。 */
    @Override
    public TenantAuthorization resolve(TenantAuthorizationRequest request) {
        return request.systemTenant() ? resolveSystem(request) : resolveOrdinary(request);
    }

    /** 解析系统租户授权：super_admin 拥有全部权限，common 无权限。 */
    private TenantAuthorization resolveSystem(TenantAuthorizationRequest request) {
        return switch (request.membershipRole()) {
            case "super_admin" -> resolveSuperAdmin(request);
            case "common" -> new TenantAuthorization(Set.of(), packages.systemPackage());
            default -> throw invalidMembershipRole();
        };
    }

    /** 解析系统租户 super_admin 的授权：合并平台与租户的全部启用权限。 */
    private TenantAuthorization resolveSuperAdmin(TenantAuthorizationRequest request) {
        var platform = platforms.resolve(request.accountId());
        if (!request.tenantId().equals(platform.tenantId())) {
            throw new DomainException(ErrorCodeConstants.SYSTEM_TENANT_CONTEXT_INVALID);
        }
        Set<String> permissions = Stream.concat(
                        catalog.activeCodes(ManagementPermissionScope.PLATFORM).stream(),
                        catalog.activeCodes(ManagementPermissionScope.TENANT).stream())
                .map(ManagementPermissionCode::value)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new TenantAuthorization(permissions, packages.systemPackage());
    }

    /** 解析普通租户授权：tenant_admin 拥有套餐权限，common 无权限。 */
    private TenantAuthorization resolveOrdinary(TenantAuthorizationRequest request) {
        TenantPackageView tenantPackage = activeOrdinaryPackage(request);
        return switch (request.membershipRole()) {
            case "tenant_admin" -> new TenantAuthorization(packagePermissions(tenantPackage), tenantPackage);
            case "common" -> new TenantAuthorization(Set.of(), tenantPackage);
            default -> throw invalidMembershipRole();
        };
    }

    /** 获取普通租户绑定的激活套餐，缺少套餐时抛出领域异常。 */
    private TenantPackageView activeOrdinaryPackage(TenantAuthorizationRequest request) {
        if (request.packageId() == null || request.packageId() <= 0) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_MISSING);
        }
        return packages.getActive(request.packageId());
    }

    /** 校验并转换套餐声明的权限码为租户作用域权限集合。 */
    private Set<String> packagePermissions(TenantPackageView tenantPackage) {
        return catalog.validate(tenantPackage.permissionCodes(), ManagementPermissionScope.TENANT).stream()
                .map(ManagementPermissionCode::value)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** 构造成员角色无效的领域异常。 */
    private static DomainException invalidMembershipRole() {
        return new DomainException(ErrorCodeConstants.TENANT_MEMBERSHIP_ROLE_INVALID);
    }
}
