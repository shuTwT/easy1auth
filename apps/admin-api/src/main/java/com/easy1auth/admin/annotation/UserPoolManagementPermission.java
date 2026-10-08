package com.easy1auth.admin.annotation;

import com.easy1auth.admin.security.ManagementRouteInventory;
import com.easy1auth.admin.security.UserPoolSecurityFilter;
import com.easy1auth.system.constant.ManagementPermissionCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户池级管理权限注解（标注在方法上）。
 *
 * <p>用于标注需要"用户池内"权限的管理接口：请求必须通过用户池上下文校验，且当前账号
 * 在该用户池内具备 {@link ManagementPermissionCode} 指定的权限。权限校验由
 * {@link UserPoolSecurityFilter} 与 {@link ManagementRouteInventory} 协同完成。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UserPoolManagementPermission {
    /** 要求的用户池级权限编码，其 scope 必须为 USER_POOL */
    ManagementPermissionCode value();
}
