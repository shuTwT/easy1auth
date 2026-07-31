package com.easy1auth.admin.web;

import com.easy1auth.admin.config.AdminJwtProperties;
import com.easy1auth.adminidentity.AdminIdentityService;
import com.easy1auth.adminidentity.AuthenticatedAdmin;
import com.easy1auth.tenant.TenantService;
import com.easy1auth.tenant.TenantSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PublicRegistrationService {
    private final AdminIdentityService identities;
    private final TenantService tenants;
    private final AdminJwtProperties jwt;

    PublicRegistrationService(AdminIdentityService identities, TenantService tenants, AdminJwtProperties jwt) {
        this.identities = identities;
        this.tenants = tenants;
        this.jwt = jwt;
    }

    @Transactional
    RegistrationResult register(String username, String email, String password, String code, String userAgent, String ip) {
        AuthenticatedAdmin identity = identities.register(username, email, password, code, jwt.refreshTtl(), userAgent, ip);
        TenantSummary tenant = tenants.createOrdinaryWithDefaultPackage(username + "的租户", identity.account().id());
        return new RegistrationResult(identity, tenant);
    }

    record RegistrationResult(AuthenticatedAdmin identity, TenantSummary tenant) {
    }
}
