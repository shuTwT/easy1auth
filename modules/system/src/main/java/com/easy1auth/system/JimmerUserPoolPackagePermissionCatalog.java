package com.easy1auth.system;

import com.easy1auth.system.constant.ManagementPermissionCode;
import com.easy1auth.system.constant.ManagementPermissionScope;
import com.easy1auth.tenant.UserPoolPackagePermissionCatalog;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 用户池套餐权限目录适配（包私有）。
 *
 * <p>将 system 域的管理端权限目录适配为用户池套餐模块所需的
 * {@link UserPoolPackagePermissionCatalog} 接口：负责校验用户池作用域的权限码
 * （套餐授权用），并提供平台作用域的全部启用权限码。</p>
 */
@Component
final class JimmerUserPoolPackagePermissionCatalog implements UserPoolPackagePermissionCatalog {
    /** 管理端权限目录 */
    private final ManagementPermissionCatalog catalog;

    JimmerUserPoolPackagePermissionCatalog(ManagementPermissionCatalog catalog) {
        this.catalog = catalog;
    }

    /** 校验并返回用户池作用域下仍有效的权限码（套餐权限保存前校验）。 */
    @Override
    public List<String> validateActiveUserPoolPermissionCodes(Collection<String> permissionCodes) {
        return catalog.validate(permissionCodes, ManagementPermissionScope.USER_POOL).stream()
                .map(ManagementPermissionCode::value)
                .toList();
    }

    /** 返回平台作用域下全部启用权限码。 */
    @Override
    public List<String> activePlatformPermissionCodes() {
        return catalog.activeCodes(ManagementPermissionScope.PLATFORM).stream()
                .map(ManagementPermissionCode::value)
                .toList();
    }
}
