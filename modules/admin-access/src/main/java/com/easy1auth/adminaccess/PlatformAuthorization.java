package com.easy1auth.adminaccess;

import java.util.Set;
import java.util.UUID;

public record PlatformAuthorization(UUID tenantId, Set<ManagementPermissionCode> permissionCodes) {
    public PlatformAuthorization {
        permissionCodes = Set.copyOf(permissionCodes);
    }

    public boolean has(ManagementPermissionCode permission) {
        return permissionCodes.contains(permission);
    }
}
