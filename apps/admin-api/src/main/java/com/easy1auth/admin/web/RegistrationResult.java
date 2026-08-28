package com.easy1auth.admin.web;

import com.easy1auth.adminidentity.AuthenticatedAdmin;
import com.easy1auth.tenant.TenantSummary;

/** 管理员注册成功后返回的身份与租户信息。 */
record RegistrationResult(AuthenticatedAdmin identity, TenantSummary tenant) {
    }
