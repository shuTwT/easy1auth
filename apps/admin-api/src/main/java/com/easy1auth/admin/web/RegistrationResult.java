package com.easy1auth.admin.web;

import com.easy1auth.adminidentity.AuthenticatedAdmin;
import com.easy1auth.tenant.TenantSummary;

record RegistrationResult(AuthenticatedAdmin identity, TenantSummary tenant) {
    }
