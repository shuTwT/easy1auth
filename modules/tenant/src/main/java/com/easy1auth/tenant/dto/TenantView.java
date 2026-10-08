package com.easy1auth.tenant.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantView(UUID id, UUID userPoolId, String name, String code, String description,
                         String status, Instant createdAt, Instant updatedAt) {
}
