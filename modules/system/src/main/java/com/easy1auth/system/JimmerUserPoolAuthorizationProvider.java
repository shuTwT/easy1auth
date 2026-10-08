package com.easy1auth.system;

import com.easy1auth.system.constant.ErrorCodeConstants;
import com.easy1auth.system.constant.ManagementPermissionCode;
import com.easy1auth.system.constant.ManagementPermissionScope;
import com.easy1auth.system.model.AdminAccountEntityTable;
import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.tenant.dto.UserPoolAuthorization;
import com.easy1auth.tenant.UserPoolAuthorizationProvider;
import com.easy1auth.tenant.dto.UserPoolAuthorizationRequest;
import com.easy1auth.tenant.service.UserPoolPackageService;
import com.easy1auth.tenant.dto.UserPoolPackageView;
import com.easy1auth.system.repository.AdminAccessRepository;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 用户池授权提供者的 jimmer 实现（包私有）。
 *
 * <p>基于 system 域的权限目录解析用户池授权：系统用户池的 super_admin 合并
 * 平台与用户池的全部启用权限；普通用户池按套餐声明的权限码解析，tenant_admin 获得
 * 套餐权限，common 无任何权限。</p>
 */
@Component
final class JimmerUserPoolAuthorizationProvider implements UserPoolAuthorizationProvider {
    /** admin_account 表静态描述符 */
    private static final AdminAccountEntityTable ACCOUNT = AdminAccountEntityTable.$;

    /** jimmer SQL 客户端 */
    private final AdminAccessRepository repository;
    /** 管理端权限目录 */
    private final ManagementPermissionCatalog catalog;
    /** 用户池套餐服务 */
    private final UserPoolPackageService packages;
    /** 平台授权解析器（解析 super_admin 的平台权限） */
    private final PlatformAuthorizationResolver platforms;

    JimmerUserPoolAuthorizationProvider(
            AdminAccessRepository repository,
            ManagementPermissionCatalog catalog,
            UserPoolPackageService packages,
            PlatformAuthorizationResolver platforms) {
        this.repository = repository;
        this.catalog = catalog;
        this.packages = packages;
        this.platforms = platforms;
    }

    /** 判断账号是否为激活状态（授权解析的前置校验）。 */
    @Override
    public boolean isActiveAccount(UUID accountId) {
        return repository.sql().createQuery(ACCOUNT)
                .where(ACCOUNT.id().eq(accountId), ACCOUNT.status().eq("active"))
                .select(ACCOUNT.id())
                .exists();
    }

    /** 解析用户池授权：系统用户池与普通用户池走不同逻辑。 */
    @Override
    public UserPoolAuthorization resolve(UserPoolAuthorizationRequest request) {
        return request.systemUserPool() ? resolveSystem(request) : resolveOrdinary(request);
    }

    /** 解析系统用户池授权：super_admin 拥有全部权限，common 无权限。 */
    private UserPoolAuthorization resolveSystem(UserPoolAuthorizationRequest request) {
        return switch (request.membershipRole()) {
            case "super_admin" -> resolveSuperAdmin(request);
            case "common" -> new UserPoolAuthorization(Set.of(), packages.systemPackage());
            default -> throw invalidMembershipRole();
        };
    }

    /** 解析系统用户池 super_admin 的授权：合并平台与用户池的全部启用权限。 */
    private UserPoolAuthorization resolveSuperAdmin(UserPoolAuthorizationRequest request) {
        var platform = platforms.resolve(request.accountId());
        if (!request.userPoolId().equals(platform.userPoolId())) {
            throw new DomainException(ErrorCodeConstants.SYSTEM_USER_POOL_CONTEXT_INVALID);
        }
        Set<String> permissions = Stream.concat(
                        catalog.activeCodes(ManagementPermissionScope.PLATFORM).stream(),
                        catalog.activeCodes(ManagementPermissionScope.USER_POOL).stream())
                .map(ManagementPermissionCode::value)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new UserPoolAuthorization(permissions, packages.systemPackage());
    }

    /** 解析普通用户池授权：tenant_admin 拥有套餐权限，common 无权限。 */
    private UserPoolAuthorization resolveOrdinary(UserPoolAuthorizationRequest request) {
        UserPoolPackageView userPoolPackage = activeOrdinaryPackage(request);
        return switch (request.membershipRole()) {
            case "tenant_admin" -> new UserPoolAuthorization(packagePermissions(userPoolPackage), userPoolPackage);
            case "common" -> new UserPoolAuthorization(Set.of(), userPoolPackage);
            default -> throw invalidMembershipRole();
        };
    }

    /** 获取普通用户池绑定的激活套餐，缺少套餐时抛出领域异常。 */
    private UserPoolPackageView activeOrdinaryPackage(UserPoolAuthorizationRequest request) {
        if (request.packageId() == null || request.packageId() <= 0) {
            throw new DomainException(ErrorCodeConstants.USER_POOL_PACKAGE_MISSING);
        }
        return packages.getActive(request.packageId());
    }

    /** 校验并转换套餐声明的权限码为用户池作用域权限集合。 */
    private Set<String> packagePermissions(UserPoolPackageView userPoolPackage) {
        return catalog.validate(userPoolPackage.permissionCodes(), ManagementPermissionScope.USER_POOL).stream()
                .map(ManagementPermissionCode::value)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** 构造成员角色无效的领域异常。 */
    private static DomainException invalidMembershipRole() {
        return new DomainException(ErrorCodeConstants.USER_POOL_MEMBERSHIP_ROLE_INVALID);
    }
}
