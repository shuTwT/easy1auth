package com.easy1auth.audit.dto;
/** 审计统计视图。 */
public record AuditStats(long totalLogs, long successLogs, long failedLogs, long todayLogs) { }
