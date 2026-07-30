package com.easy1auth.tenant;

import java.time.Instant;
import java.util.List;

public record TenantPackageView(
        long id,
        String code,
        String name,
        String status,
        boolean defaultPackage,
        int maxUsers,
        int maxApps,
        List<String> permissionCodes,
        Instant createdAt,
        Instant updatedAt
) {
    public TenantPackageView {
        if (id < 0) {
            throw new IllegalArgumentException("package id must not be negative");
        }
        permissionCodes = List.copyOf(permissionCodes);
    }
}
