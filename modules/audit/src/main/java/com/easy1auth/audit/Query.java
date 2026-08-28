package com.easy1auth.audit;
import java.time.Instant;
/** 审计事件查询条件。 */
public record Query(String actorName, String eventType, String action, String outcome, Instant start, Instant end) { }
