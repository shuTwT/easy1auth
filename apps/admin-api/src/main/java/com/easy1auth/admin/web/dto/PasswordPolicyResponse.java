package com.easy1auth.admin.web.dto;

import com.easy1auth.security.dto.PolicyView;

public record PasswordPolicyResponse(PolicyView policyView, ExpiryStatus expiryStatus) {
}
