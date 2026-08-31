package com.easy1auth.connection.dto;

/**
 * 身份源数量统计视图。
 */
public record EnterpriseIdentityStatsView(long totalSources, long activeSources, long inactiveSources) {
}
