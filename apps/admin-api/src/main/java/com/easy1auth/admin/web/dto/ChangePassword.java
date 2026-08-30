package com.easy1auth.admin.web.dto;

public record ChangePassword(String currentPassword, String newPassword, String confirmPassword) {
}
