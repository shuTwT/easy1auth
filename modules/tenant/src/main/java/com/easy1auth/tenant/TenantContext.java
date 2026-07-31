package com.easy1auth.tenant;

import java.util.Set;
import java.util.UUID;

public record TenantContext(
        UUID accountId, UUID tenantId, UUID membershipId, String membershipRole,
        Set<String> permissions, TenantPackageView tenantPackage,
        String traceId) {
    public TenantContext {
        permissions = Set.copyOf(permissions);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
