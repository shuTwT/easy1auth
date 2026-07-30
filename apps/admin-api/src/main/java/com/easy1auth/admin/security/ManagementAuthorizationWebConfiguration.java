package com.easy1auth.admin.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ManagementAuthorizationWebConfiguration implements WebMvcConfigurer {
    private final TenantManagementPermissionInterceptor tenantPermissions;

    ManagementAuthorizationWebConfiguration(TenantManagementPermissionInterceptor tenantPermissions) {
        this.tenantPermissions = tenantPermissions;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantPermissions).order(0);
    }
}
