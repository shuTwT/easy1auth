package com.easy1auth.admin.web.dto;

public record ImportError(int row, String username, String error) {
}
