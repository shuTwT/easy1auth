package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;

/** 权限详情视图。 */
public record PermissionView(UUID id, UUID tenantId, String code, String name, String description, String type,
                                 String resource, String action, UUID parentId, Instant createdAt, Instant updatedAt,
                                 ParentSummary parent) {
    }
