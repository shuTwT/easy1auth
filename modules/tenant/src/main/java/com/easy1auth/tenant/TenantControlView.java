package com.easy1auth.tenant;

import java.util.UUID;

public record TenantControlView(
        UUID id,
        String name,
        String status,
        TenantPackageView tenantPackage,
        UUID administratorAccountId) {
}
