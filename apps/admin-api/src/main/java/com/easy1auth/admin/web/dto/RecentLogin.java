package com.easy1auth.admin.web.dto;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record RecentLogin(String username, @Nullable String email, String ip, Instant time, String status) {
}
