package com.easy1auth.poolidentity.service;
import java.time.Instant;
import java.util.UUID;
/** 用户组详情视图。 */
public record GroupView(UUID id, UUID tenantId, String name, String description, String type, UUID parentId,
                        Instant createdAt, Instant updatedAt, Counts _count) { }
