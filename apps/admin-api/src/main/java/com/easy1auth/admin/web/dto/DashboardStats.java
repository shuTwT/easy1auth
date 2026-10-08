package com.easy1auth.admin.web.dto;

public record DashboardStats(long userPoolCount, long userCount, long applicationCount, long todayLoginCount) {
}
