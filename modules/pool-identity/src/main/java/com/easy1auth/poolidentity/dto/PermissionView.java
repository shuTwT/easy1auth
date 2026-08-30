package com.easy1auth.poolidentity.dto;
import java.util.*;
import java.time.*;

/** 权限详情视图。 */
public record PermissionView(UUID id, UUID tenantId, String code, String name, String description, String type,
                                 String resource, String action, UUID parentId, Instant createdAt, Instant updatedAt,
                                 ParentSummary parent) {
    }
