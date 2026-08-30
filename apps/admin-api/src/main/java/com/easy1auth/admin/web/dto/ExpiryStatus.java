package com.easy1auth.admin.web.dto;

public record ExpiryStatus(boolean expired, int daysUntilExpiry) {
}
