package com.easy1auth.tenant.dto;

import com.easy1auth.tenant.model.UserPoolEntity;

/** 用户池状态视图。 */
public record UserPoolState(UserPoolEntity tenant, String role) {
}
