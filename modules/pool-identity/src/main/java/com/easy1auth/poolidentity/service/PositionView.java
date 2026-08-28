package com.easy1auth.poolidentity.service;
import java.time.Instant;
import java.util.UUID;
/** 岗位详情视图。 */
public record PositionView(UUID id, UUID tenantId, String name, String code, String description, UUID departmentId,
                           int level, String sequence, long userCount, Integer maxCount, Instant createdAt,
                           Instant updatedAt) { }
