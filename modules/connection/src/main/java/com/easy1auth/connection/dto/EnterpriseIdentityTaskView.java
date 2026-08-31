package com.easy1auth.connection.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 企业身份同步任务视图。
 */
public record EnterpriseIdentityTaskView(UUID id, String type, String status, Map<String, Object> summary,
                                         String lastError, Instant createdAt, Instant finishedAt) {
}
