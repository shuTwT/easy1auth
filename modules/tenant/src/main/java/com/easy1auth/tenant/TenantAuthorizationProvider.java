package com.easy1auth.tenant;
import java.util.Set; import java.util.UUID;
/** Port implemented by admin-access; keeps tenant independent from foreign repositories. */
public interface TenantAuthorizationProvider{
 boolean isActiveAccount(UUID accountId);
 Set<String> roles(UUID membershipId,String membershipRole);
 Set<String> permissions(UUID membershipId,String membershipRole);
}
