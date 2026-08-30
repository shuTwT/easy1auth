package com.easy1auth.security.dto;

import java.util.UUID;

/** 邮箱认证挑战消费结果。 */
public record ConsumedEmailChallenge(UUID subjectId, String destination) {
}
