package com.easy1auth.poolidentity.service;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
/** 最近成功登录记录。 */
public record RecentLogin(String username, @Nullable String email, Instant time) { }
