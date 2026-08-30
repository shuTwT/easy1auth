package com.easy1auth.admin.web.dto;

import java.util.List;

public record AuditStatsResponse(long totalLogs, long successLogs, long failedLogs, long todayLogs,
                                 long weekLogs, long monthLogs, List<?> topActions, List<?> topUsers, List<?> topIps) {
}
