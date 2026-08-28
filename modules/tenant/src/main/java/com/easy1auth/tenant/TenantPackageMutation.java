package com.easy1auth.tenant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 租户套餐变更请求（接口入参 DTO）。
 *
 * <p>承载创建/更新套餐时提交的字段。创建时 defaultPackage 由调用方指定，
 * 更新时服务层会沿用现有默认标记（不覆盖）。</p>
 *
 * @param code            套餐编码（全局唯一，1-100 字符）
 * @param name            套餐名称（1-100 字符）
 * @param defaultPackage  是否为默认套餐
 * @param maxUsers        允许的最大用户数（须为正数）
 * @param maxApps         允许的最大应用数（须为正数）
 * @param permissionCodes 套餐权限编码列表（可为 null；构造时复制为不可修改列表）
 */
public record TenantPackageMutation(
        String code,
        String name,
        boolean defaultPackage,
        int maxUsers,
        int maxApps,
        List<String> permissionCodes
) {
    public TenantPackageMutation {
        permissionCodes = permissionCodes == null ? null : Collections.unmodifiableList(new ArrayList<>(permissionCodes));
    }
}
