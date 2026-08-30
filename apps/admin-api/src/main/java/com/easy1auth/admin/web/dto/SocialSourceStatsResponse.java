package com.easy1auth.admin.web.dto;

import java.util.Map;

public record SocialSourceStatsResponse(long totalSources, long activeSources, long inactiveSources,
                                        Map<String, Long> byType) {
}
