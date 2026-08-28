package com.easy1auth.enterpriseidentity;

/**
 * 身份源数量统计视图。
 */
public record EnterpriseIdentityStats(long totalSources, long activeSources, long inactiveSources) {
}
