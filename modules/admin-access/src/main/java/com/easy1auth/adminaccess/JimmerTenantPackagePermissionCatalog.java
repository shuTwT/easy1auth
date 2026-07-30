package com.easy1auth.adminaccess;

import com.easy1auth.tenant.TenantPackagePermissionCatalog;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
final class JimmerTenantPackagePermissionCatalog implements TenantPackagePermissionCatalog {
    private final ManagementPermissionCatalog catalog;

    JimmerTenantPackagePermissionCatalog(ManagementPermissionCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<String> validateActiveTenantPermissionCodes(Collection<String> permissionCodes) {
        return catalog.validate(permissionCodes, ManagementPermissionScope.TENANT).stream()
                .map(ManagementPermissionCode::value)
                .toList();
    }

    @Override
    public List<String> activePlatformPermissionCodes() {
        return catalog.activeCodes(ManagementPermissionScope.PLATFORM).stream()
                .map(ManagementPermissionCode::value)
                .toList();
    }
}
