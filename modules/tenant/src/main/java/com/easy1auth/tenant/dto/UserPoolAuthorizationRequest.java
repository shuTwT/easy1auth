package com.easy1auth.tenant.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * 用户池授权解析请求（只读 DTO）。
 *
 * <p>携带解析用户池授权所需的全部输入：账号、用户池、成员关系、角色与套餐，
 *
 * @param accountId       管理账号 ID
 * @param userPoolId        用户池 ID
 * @param membershipId    账号在该用户池下的成员关系 ID
 * @param membershipRole  账号在该用户池内的角色：super_admin / tenant_admin / common
 * @param systemUserPool    用户池是否为系统用户池
 * @param packageId       用户池绑定的套餐 ID（系统用户池可为 null）
 */
public record UserPoolAuthorizationRequest(
        UUID accountId,
        UUID userPoolId,
        UUID membershipId,
        String membershipRole,
        boolean systemUserPool,
        Long packageId) {
    public UserPoolAuthorizationRequest {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(userPoolId, "userPoolId");
        Objects.requireNonNull(membershipId, "membershipId");
        Objects.requireNonNull(membershipRole, "membershipRole");
    }
}
