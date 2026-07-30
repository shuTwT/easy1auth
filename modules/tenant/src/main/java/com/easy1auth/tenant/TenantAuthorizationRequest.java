package com.easy1auth.tenant;

import java.util.Objects;
import java.util.UUID;

public record TenantAuthorizationRequest(
        UUID accountId,
        UUID tenantId,
        UUID membershipId,
        String membershipRole,
        boolean systemTenant,
        Long packageId) {
    public TenantAuthorizationRequest {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(membershipId, "membershipId");
        Objects.requireNonNull(membershipRole, "membershipRole");
    }
}
