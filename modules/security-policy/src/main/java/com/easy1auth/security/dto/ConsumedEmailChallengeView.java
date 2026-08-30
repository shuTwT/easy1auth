package com.easy1auth.security.dto;

import java.util.UUID;

/** 邮箱认证挑战消费结果。 */
public record ConsumedEmailChallengeView(UUID subjectId, String destination) {
}
