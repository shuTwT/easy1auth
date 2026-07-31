package com.easy1auth.tenant;

import java.util.UUID;

/**
 * Port implemented by admin-access; keeps tenant independent from foreign repositories.
 */
public interface TenantAuthorizationProvider {
    boolean isActiveAccount(UUID accountId);

    TenantAuthorization resolve(TenantAuthorizationRequest request);
}
