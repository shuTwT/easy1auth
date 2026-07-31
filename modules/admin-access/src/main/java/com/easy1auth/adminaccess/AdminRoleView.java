package com.easy1auth.adminaccess;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminRoleView(UUID id, UUID tenantId, String name, String description, List<String> permissions,
                            boolean isSystem, long adminCount, Instant createdAt, Instant updatedAt) {
}
