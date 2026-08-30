package com.easy1auth.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** OAuth2 应用只读视图。 */
public record ApplicationView(UUID id, UUID tenantId, String name, String logo, String description, String type,
                              String clientId, String clientSecret, List<String> redirectUris,
                              List<String> postLogoutRedirectUris, List<String> allowedGrantTypes,
                              List<String> scopes, boolean requirePkce, boolean requireConsent,
                              int accessTokenLifetime, int refreshTokenLifetime, String status, Instant createdAt,
                              Instant updatedAt) {
}
