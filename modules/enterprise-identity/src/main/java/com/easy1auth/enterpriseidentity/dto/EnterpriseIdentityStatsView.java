package com.easy1auth.enterpriseidentity.dto;

/**
 * 身份源数量统计视图。
 */
public record EnterpriseIdentityStatsView(long totalSources, long activeSources, long inactiveSources) {
}
