package com.easy1auth.tenant.dto;

import java.util.UUID;

/**
 * 租户摘要视图（面向接口层的只读 DTO）。
 *
 * <p>用于租户列表、切换租户等场景，描述一个租户的关键信息以及当前账号
 * 在该租户中的角色。</p>
 *
 * @param id           租户 ID
 * @param name         租户名称
 * @param status       租户状态：active / suspended / deleted
 * @param system       是否系统租户
 * @param tenantPackage 绑定的租户套餐摘要
 * @param role         当前账号在该租户内的角色：super_admin / tenant_admin / common
 */
public record TenantSummary(UUID id, String name, String status, boolean system, TenantPackageView tenantPackage,
                            String role) {
}
