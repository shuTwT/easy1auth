package com.easy1auth.tenant.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * 租户授权解析请求（只读 DTO）。
 *
 * <p>携带解析租户授权所需的全部输入：账号、租户、成员关系、角色与套餐，
 *
 * @param accountId       管理账号 ID
 * @param tenantId        租户 ID
 * @param membershipId    账号在该租户下的成员关系 ID
 * @param membershipRole  账号在该租户内的角色：super_admin / tenant_admin / common
 * @param systemTenant    租户是否为系统租户
 * @param packageId       租户绑定的套餐 ID（系统租户可为 null）
 */
public record TenantAuthorizationRequest(
        UUID accountId,
        UUID tenantId,
        UUID membershipId,
        String membershipRole,
        boolean systemTenant,
        Long packageId) {
    public TenantAuthorizationRequest {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(membershipId, "membershipId");
        Objects.requireNonNull(membershipRole, "membershipRole");
    }
}
