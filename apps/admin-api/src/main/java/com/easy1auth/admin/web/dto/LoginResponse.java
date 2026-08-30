package com.easy1auth.admin.web.dto;

import java.util.List;

public record LoginResponse(String status, String token, String refreshToken, LoginUser user, List<?> tenants,
                            String challengeToken, List<String> methods, Integer expiresIn) {
    public static LoginResponse success(String token, String refresh, LoginUser user, List<?> tenants) {
        return new LoginResponse("success", token, refresh, user, tenants, null, null, null);
    }

    public static LoginResponse mfa(String challenge, int expires) {
        return new LoginResponse("mfa_required", null, null, null, List.of(), challenge, List.of("totp"), expires);
    }
}
