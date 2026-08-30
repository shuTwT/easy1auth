package com.easy1auth.admin.web.dto;

public record EmailVerifyRequest(String challengeToken, String code) {
}
