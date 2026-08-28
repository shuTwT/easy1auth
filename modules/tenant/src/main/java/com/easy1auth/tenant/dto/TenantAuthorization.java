package com.easy1auth.tenant.dto;

import com.easy1auth.tenant.TenantAuthorizationProvider;

import java.util.Set;

/**
 * 租户授权结果视图（只读 DTO）。
 *
 * <p>由 {@link TenantAuthorizationProvider} 在解析租户上下文时生成，
 * 包含账号在指定租户下拥有的权限集合与租户绑定的套餐信息。</p>
 *
 * @param permissions   账号在该租户下拥有的权限编码集合（构造时复制为不可修改集合）
 * @param tenantPackage 租户绑定的套餐摘要，不可为空
 */
public record TenantAuthorization(
        Set<String> permissions,
        TenantPackageView tenantPackage) {
    public TenantAuthorization {
        permissions = Set.copyOf(permissions);
        java.util.Objects.requireNonNull(tenantPackage, "tenantPackage");
    }
}
