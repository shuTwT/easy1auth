package com.easy1auth.admin.web.dto;

public record LoginRequest(String username, String password, String email, String code, String challengeToken,
                           String loginType) {
}
