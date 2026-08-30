package com.easy1auth.security.dto;

/** 安全策略值对象。 */
public record PolicyView(int minLength, boolean requireUpper, boolean requireLower, boolean requireNumber,
                         boolean requireSpecial, int maxAgeDays, int historyCount, boolean mfaRequired,
                         int loginAttemptLimit, int lockoutSeconds) {
}
