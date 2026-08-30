package com.easy1auth.poolidentity.dto;

/** 权限统计视图。 */
public record PermissionStats(long totalPermissions, long menuPermissions, long operationPermissions, long dataPermissions) {
    }
