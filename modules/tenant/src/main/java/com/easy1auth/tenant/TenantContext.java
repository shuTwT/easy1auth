package com.easy1auth.tenant;

import java.util.Set;
import java.util.UUID;

public record TenantContext(
        UUID accountId, UUID tenantId, UUID membershipId, String membershipRole,
        Set<String> roles, Set<String> permissions, String traceId) {
    public TenantContext { roles = Set.copyOf(roles); permissions = Set.copyOf(permissions); }
    public boolean hasPermission(String permission) { return permissions.contains("*") || permissions.contains(permission); }
}
