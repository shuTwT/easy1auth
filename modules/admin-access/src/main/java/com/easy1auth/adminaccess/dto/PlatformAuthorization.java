package com.easy1auth.adminaccess.dto;

import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.adminaccess.PlatformAuthorizationResolver;

import java.util.Set;
import java.util.UUID;

/**
 * 平台授权结果（不可变值对象）。
 *
 * <p>由 {@link PlatformAuthorizationResolver} 生成，描述管理账号可访问的平台
 * 资源以及持有的平台权限码集合；构造时对权限集合做不可变拷贝，防止外部修改。</p>
 *
 * @param tenantId        平台对应的系统租户 ID
 * @param permissionCodes 账号持有的平台权限码集合
 */
public record PlatformAuthorization(UUID tenantId, Set<ManagementPermissionCode> permissionCodes) {
    public PlatformAuthorization {
        permissionCodes = Set.copyOf(permissionCodes);
    }

    /** 判断是否持有指定权限码。 */
    public boolean has(ManagementPermissionCode permission) {
        return permissionCodes.contains(permission);
    }
}
