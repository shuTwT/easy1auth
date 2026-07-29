package com.easy1auth.adminidentity;

import java.time.Instant;
import java.util.UUID;

public record AdminAccount(UUID id, String username, String email, String phone, String status,
                           long securityVersion, UUID lastTenantId, boolean mfaEnabled,
                           String mfaType, Instant lastLoginAt, Instant createdAt, Instant updatedAt) {}
