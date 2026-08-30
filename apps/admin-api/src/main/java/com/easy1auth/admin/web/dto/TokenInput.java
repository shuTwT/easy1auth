package com.easy1auth.admin.web.dto;

public record TokenInput(String token, String challengeToken, String type) {
}
