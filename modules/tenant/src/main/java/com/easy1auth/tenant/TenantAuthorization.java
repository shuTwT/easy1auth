package com.easy1auth.tenant;

import java.util.Objects;
import java.util.Set;

public record TenantAuthorization(
        Set<String> permissions,
        TenantDataBoundary dataBoundary,
        TenantPackageView tenantPackage) {
    public TenantAuthorization {
        permissions = Set.copyOf(permissions);
        Objects.requireNonNull(dataBoundary, "dataBoundary");
        Objects.requireNonNull(tenantPackage, "tenantPackage");
        if (dataBoundary == TenantDataBoundary.NONE && !permissions.isEmpty()) {
            throw new IllegalArgumentException("an empty data boundary cannot grant permissions");
        }
        if (dataBoundary != TenantDataBoundary.NONE && !permissions.contains(dataBoundary.code())) {
            throw new IllegalArgumentException("the data-boundary permission is required");
        }
    }
}
