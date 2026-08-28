package com.easy1auth.enterpriseidentity;

import java.time.Instant;
import java.util.UUID;

/**
 * 企业身份源视图。
 */
public record EnterpriseIdentitySourceView(UUID id, String name, String provider, String appId, String status,
                                           Instant lastSyncAt, String lastSyncStatus, String lastError,
                                           Instant createdAt, Instant updatedAt) {
}
