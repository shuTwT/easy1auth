package com.easy1auth.admin.security;

import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.tenant.TenantDataBoundary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TenantManagementPermission {
    ManagementPermissionCode value();

    TenantDataBoundary boundary();
}
