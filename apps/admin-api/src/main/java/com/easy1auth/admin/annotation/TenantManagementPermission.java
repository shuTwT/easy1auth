package com.easy1auth.admin.annotation;

import com.easy1auth.admin.security.ManagementRouteInventory;
import com.easy1auth.admin.security.TenantSecurityFilter;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 租户级管理权限注解（标注在方法上）。
 *
 * <p>用于标注需要"租户内"权限的管理接口：请求必须通过租户上下文校验，且当前账号
 * 在该租户内具备 {@link ManagementPermissionCode} 指定的权限。权限校验由
 * {@link TenantSecurityFilter} 与 {@link ManagementRouteInventory} 协同完成。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TenantManagementPermission {
    /** 要求的租户级权限编码，其 scope 必须为 TENANT */
    ManagementPermissionCode value();
}
