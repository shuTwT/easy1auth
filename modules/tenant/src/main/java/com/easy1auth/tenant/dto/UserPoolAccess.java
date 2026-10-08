package com.easy1auth.tenant.dto;

import java.util.Set;
import java.util.UUID;

/**
 * 管理账号在指定用户池下的有效访问结果。
 *
 * @param userPoolId       用户池 ID
 * @param membershipRole 成员角色
 * @param permissions    有效权限编码集合
 * @param userPoolPackage  用户池套餐
 */
public record UserPoolAccess(
        UUID userPoolId,
        String membershipRole,
        Set<String> permissions,
        UserPoolPackageView userPoolPackage) {
    public UserPoolAccess {
        permissions = Set.copyOf(permissions);
    }
}
