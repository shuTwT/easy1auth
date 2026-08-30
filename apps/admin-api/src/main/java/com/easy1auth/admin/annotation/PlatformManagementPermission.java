package com.easy1auth.admin.annotation;

import com.easy1auth.admin.security.ManagementRouteInventory;
import com.easy1auth.admin.security.TenantSecurityFilter;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 平台级管理权限注解（标注在方法上）。
 *
 * <p>用于标注需要"平台级"权限的管理接口（如平台运营、系统租户操作），不依赖具体租户
 * 上下文，仅要求当前账号具备 {@link ManagementPermissionCode} 指定的平台权限。权限校验
 * 由 {@link TenantSecurityFilter} 与 {@link ManagementRouteInventory} 协同完成。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PlatformManagementPermission {
    /** 要求的平台级权限编码，其 scope 必须为 PLATFORM 且类型为 ACTION */
    ManagementPermissionCode value();
}
