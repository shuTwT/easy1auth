package com.easy1auth.adminaccess.constant;

/**
 * 管理端权限作用域枚举。
 *
 * <p>标识一条权限归属的授权范围：PLATFORM（平台管理，仅系统租户 super_admin
 * 持有）、TENANT（租户管理，通过管理角色或租户套餐授予）。权限目录查询与
 * 授权解析均以此区分作用域。</p>
 */
public enum ManagementPermissionScope {
    PLATFORM,
    TENANT
}
