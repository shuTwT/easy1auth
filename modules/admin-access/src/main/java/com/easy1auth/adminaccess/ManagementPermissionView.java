package com.easy1auth.adminaccess;

public record ManagementPermissionView(
        String code,
        ManagementPermissionType type,
        ManagementPermissionScope scope,
        String name,
        String parentCode,
        String resource,
        String action,
        int sortOrder,
        boolean active) {
}
