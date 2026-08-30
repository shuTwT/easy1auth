package com.easy1auth.admin.web.dto;

import com.easy1auth.adminidentity.dto.AuthenticatedAdmin;
import com.easy1auth.tenant.dto.TenantSummary;

/** 管理员注册成功后返回的身份与租户信息。 */
public record RegistrationResult(AuthenticatedAdmin identity, TenantSummary tenant) {
}
