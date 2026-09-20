package com.easy1auth.tenant.dto;

import java.util.Set;
import java.util.UUID;

/**
 * 管理账号在指定租户下的有效访问结果。
 *
 * @param tenantId       租户 ID
 * @param membershipRole 成员角色
 * @param permissions    有效权限编码集合
 * @param tenantPackage  租户套餐
 */
public record TenantAccess(
        UUID tenantId,
        String membershipRole,
        Set<String> permissions,
        TenantPackageView tenantPackage) {
    public TenantAccess {
        permissions = Set.copyOf(permissions);
    }
}
