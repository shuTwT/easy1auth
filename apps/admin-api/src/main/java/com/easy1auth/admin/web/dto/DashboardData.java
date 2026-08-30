package com.easy1auth.admin.web.dto;

import java.util.List;

public record DashboardData(DashboardStats stats, List<RecentLogin> recentLogins) {
}
