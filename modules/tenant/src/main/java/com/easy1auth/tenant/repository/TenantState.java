package com.easy1auth.tenant.repository;

import com.easy1auth.tenant.model.TenantEntity;

/** 租户状态视图。 */
public record TenantState(TenantEntity tenant, String role) {
}
