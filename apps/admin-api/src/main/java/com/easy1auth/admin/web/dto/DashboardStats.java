package com.easy1auth.admin.web.dto;

public record DashboardStats(long tenantCount, long userCount, long applicationCount, long todayLoginCount) {
}
