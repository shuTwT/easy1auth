package com.easy1auth.tenant;

import java.util.UUID;

public record TenantSummary(UUID id, String name, String status, boolean system, TenantPackageView tenantPackage, String role) {}
