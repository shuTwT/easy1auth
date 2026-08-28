package com.easy1auth.tenant;

import java.time.Instant;
import java.util.List;

/**
 * 租户套餐视图（面向接口层的只读 DTO）。
 *
 * <p>用于套餐列表、详情展示与租户绑定场景，描述套餐的配额、状态
 * 与所包含的权限编码列表。ID 为 0 表示内置系统套餐。</p>
 *
 * @param id              套餐 ID（0 表示内置系统套餐）
 * @param code            套餐编码（全局唯一）
 * @param name            套餐名称
 * @param status          套餐状态：active（启用）/ inactive（停用）
 * @param defaultPackage  是否为默认套餐（新建普通租户默认绑定）
 * @param maxUsers        套餐允许的最大用户数
 * @param maxApps         套餐允许的最大应用数
 * @param permissionCodes 套餐包含的权限编码列表（构造时复制为不可修改列表）
 * @param createdAt       创建时间
 * @param updatedAt       最后更新时间
 */
public record TenantPackageView(
        long id,
        String code,
        String name,
        String status,
        boolean defaultPackage,
        int maxUsers,
        int maxApps,
        List<String> permissionCodes,
        Instant createdAt,
        Instant updatedAt
) {
    public TenantPackageView {
        if (id < 0) {
            throw new IllegalArgumentException("package id must not be negative");
        }
        permissionCodes = List.copyOf(permissionCodes);
    }
}
