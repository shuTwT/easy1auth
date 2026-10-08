package com.easy1auth.tenant.dto;

import com.easy1auth.tenant.UserPoolAuthorizationProvider;

import java.util.Set;

/**
 * 用户池授权结果视图（只读 DTO）。
 *
 * <p>由 {@link UserPoolAuthorizationProvider} 在解析用户池上下文时生成，
 * 包含账号在指定用户池下拥有的权限集合与用户池绑定的套餐信息。</p>
 *
 * @param permissions   账号在该用户池下拥有的权限编码集合（构造时复制为不可修改集合）
 * @param userPoolPackage 用户池绑定的套餐摘要，不可为空
 */
public record UserPoolAuthorization(
        Set<String> permissions,
        UserPoolPackageView userPoolPackage) {
    public UserPoolAuthorization {
        permissions = Set.copyOf(permissions);
        java.util.Objects.requireNonNull(userPoolPackage, "userPoolPackage");
    }
}
