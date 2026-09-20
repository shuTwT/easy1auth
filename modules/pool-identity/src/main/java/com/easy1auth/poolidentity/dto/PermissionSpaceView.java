package com.easy1auth.poolidentity.dto;

import java.time.Instant;
import java.util.UUID;

public record PermissionSpaceView(UUID id, UUID tenantId, String name, String code, String description,
                                  Instant createdAt, Instant updatedAt) {
}
