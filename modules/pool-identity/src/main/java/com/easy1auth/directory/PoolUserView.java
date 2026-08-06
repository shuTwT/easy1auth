package com.easy1auth.directory;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

public record PoolUserView(UUID id, UUID tenantId, String username, @Nullable String email, String phone, String name,
                           String avatar, String status, boolean emailVerified, boolean phoneVerified,
                           String department, String position, Map<String, Object> customAttributes,
                           Instant lastLoginAt, Instant createdAt, Instant updatedAt) {
}
