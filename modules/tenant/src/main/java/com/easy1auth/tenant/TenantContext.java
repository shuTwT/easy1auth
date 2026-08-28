package com.easy1auth.tenant;

import java.util.Set;
import java.util.UUID;

/**
 * 租户上下文。
 *
 * <p>解析租户时生成并贯穿单个请求链路，携带当前账号、租户、成员关系、
 * 权限集合与套餐信息，供过滤器与领域服务进行权限判断。</p>
 *
 * @param accountId       管理账号 ID
 * @param tenantId        当前租户 ID
 * @param membershipId    账号在该租户下的成员关系 ID
 * @param membershipRole  账号在该租户内的角色：super_admin / tenant_admin / common
 * @param permissions     账号在该租户下拥有的权限编码集合（构造时复制为不可修改集合）
 * @param tenantPackage   租户绑定的套餐摘要
 * @param traceId         链路追踪 ID（透传，用于日志串联）
 */
public record TenantContext(
        UUID accountId, UUID tenantId, UUID membershipId, String membershipRole,
        Set<String> permissions, TenantPackageView tenantPackage,
        String traceId) {
    public TenantContext {
        permissions = Set.copyOf(permissions);
    }

    /** 判断账号是否拥有指定权限编码。 */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
