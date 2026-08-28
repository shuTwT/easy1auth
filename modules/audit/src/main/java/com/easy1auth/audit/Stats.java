package com.easy1auth.audit;
/** 审计统计视图。 */
public record Stats(long totalLogs, long successLogs, long failedLogs, long todayLogs) { }
