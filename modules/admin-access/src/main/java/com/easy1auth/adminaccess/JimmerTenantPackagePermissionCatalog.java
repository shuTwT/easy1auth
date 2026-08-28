package com.easy1auth.adminaccess;

import com.easy1auth.tenant.TenantPackagePermissionCatalog;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 租户套餐权限目录适配（包私有）。
 *
 * <p>将 admin-access 域的管理端权限目录适配为租户套餐模块所需的
 * {@link TenantPackagePermissionCatalog} 接口：负责校验租户作用域的权限码
 * （套餐授权用），并提供平台作用域的全部启用权限码。</p>
 */
@Component
final class JimmerTenantPackagePermissionCatalog implements TenantPackagePermissionCatalog {
    /** 管理端权限目录 */
    private final ManagementPermissionCatalog catalog;

    JimmerTenantPackagePermissionCatalog(ManagementPermissionCatalog catalog) {
        this.catalog = catalog;
    }

    /** 校验并返回租户作用域下仍有效的权限码（套餐权限保存前校验）。 */
    @Override
    public List<String> validateActiveTenantPermissionCodes(Collection<String> permissionCodes) {
        return catalog.validate(permissionCodes, ManagementPermissionScope.TENANT).stream()
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
