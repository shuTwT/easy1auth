package com.easy1auth.admin.web.dto;

public record RegisterRequest(String email, String password, String code, String username) {
}
