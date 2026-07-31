package com.easy1auth.tenant;

import java.util.Set;

public record TenantAuthorization(
        Set<String> permissions,
        TenantPackageView tenantPackage) {
    public TenantAuthorization {
        permissions = Set.copyOf(permissions);
        java.util.Objects.requireNonNull(tenantPackage, "tenantPackage");
    }
}
