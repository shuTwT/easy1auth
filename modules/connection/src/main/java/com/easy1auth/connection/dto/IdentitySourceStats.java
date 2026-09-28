package com.easy1auth.connection.dto;

import java.util.Map;

/** 企业或社会化登录身份源的完整分类统计。 */
public record IdentitySourceStats(
        long totalSources,
        long activeSources,
        long inactiveSources,
        Map<String, Long> byType
) {
}
