package com.easy1auth.admin.web.dto;

import com.easy1auth.security.dto.Policy;

public record PasswordPolicyResponse(Policy policy, ExpiryStatus expiryStatus) {
}
