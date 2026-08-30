package com.easy1auth.poolidentity.dto;

/** 角色统计视图。 */
public record RoleStats(long totalRoles, long systemRoles, long customRoles, long totalUsers) {
    }
