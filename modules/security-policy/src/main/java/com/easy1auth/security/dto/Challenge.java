package com.easy1auth.security.dto;

/** 认证挑战签发结果。 */
public record Challenge(String token, String code, int expiresIn) {
}
