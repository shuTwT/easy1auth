package com.easy1auth.poolidentity.dto;

import java.time.Instant;
import java.util.UUID;
/** 用户组详情视图。 */
public record GroupView(UUID id, UUID userPoolId, String name, String description, String type,
                        Instant createdAt, Instant updatedAt, Counts _count) {
}
