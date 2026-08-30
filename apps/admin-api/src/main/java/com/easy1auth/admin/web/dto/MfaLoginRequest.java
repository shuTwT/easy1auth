package com.easy1auth.admin.web.dto;

public record MfaLoginRequest(String challengeToken, String code) {
}
