package com.easy1auth.adminidentity;

public record AuthenticatedAdmin(AdminAccount account, String refreshToken) {}
