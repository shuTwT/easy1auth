package com.easy1auth.tenant;

import java.util.UUID;

/**
 * 租户控制视图（面向平台管理的只读 DTO）。
 *
 * <p>用于平台侧租户管理列表与详情，在租户基本信息之上额外携带
 * 当前管理员账号 ID，便于平台展示与执行管理操作。</p>
 *
 * @param id                     租户 ID
 * @param name                   租户名称
 * @param status                 租户状态：active / suspended / deleted
 * @param tenantPackage          租户绑定的套餐摘要
 * @param administratorAccountId 当前激活的管理员账号 ID（无管理员时为 null）
 */
public record TenantControlView(
        UUID id,
        String name,
        String status,
        TenantPackageView tenantPackage,
        UUID administratorAccountId) {
}
