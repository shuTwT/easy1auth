package com.easy1auth.tenant;

import java.util.Collection;
import java.util.List;

public interface TenantPackagePermissionCatalog {
    List<String> validateActiveTenantPermissionCodes(Collection<String> permissionCodes);

    List<String> activePlatformPermissionCodes();
}
