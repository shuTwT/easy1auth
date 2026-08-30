package com.easy1auth.audit.dto;
import java.util.List;
/** Webhook 订阅输入。 */
public record SubscriptionInput(String name, String url, List<String> events, Integer maxRetries, String status) { }
