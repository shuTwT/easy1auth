package com.easy1auth.poolidentity.dto;
/** 用户组统计视图。 */
public record GroupStats(long totalGroups, long teamGroups, long departmentGroups, long projectGroups,
                         long organizationGroups, long rootGroups) { }
