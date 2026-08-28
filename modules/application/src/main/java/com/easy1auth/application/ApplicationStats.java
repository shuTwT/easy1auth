package com.easy1auth.application;

/** OAuth2 应用统计视图。 */
public record ApplicationStats(long totalApplications, long activeApplications, long disabledApplications) {
}
