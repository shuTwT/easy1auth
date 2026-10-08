package com.easy1auth.admin.web.dto;

import com.easy1auth.system.dto.AuthenticatedAdmin;
import com.easy1auth.tenant.dto.UserPoolSummary;

/** 管理员注册成功后返回的身份与用户池信息。 */
public record RegistrationResult(AuthenticatedAdmin identity, UserPoolSummary tenant) {
}
