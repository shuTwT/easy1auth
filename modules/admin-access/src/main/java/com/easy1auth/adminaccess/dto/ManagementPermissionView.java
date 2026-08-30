package com.easy1auth.adminaccess.dto;

import com.easy1auth.adminaccess.constant.ManagementPermissionScope;
import com.easy1auth.adminaccess.constant.ManagementPermissionType;

/**
 * 管理端权限视图（面向接口层的只读 DTO）。
 *
 * <p>描述一条管理端权限目录记录的完整信息，供角色授权、套餐授权等界面
 * 展示与解析使用。</p>
 *
 * @param code       权限码（如 "user:list"，全局唯一）
 * @param type       权限类型：menu / directory / action
 * @param scope      权限作用域：PLATFORM（平台）/ TENANT（租户）
 * @param name       权限显示名称
 * @param parentCode 父级权限码（可为 null，用于构建树形目录）
 * @param resource   权限所属资源
 * @param action     权限动作（如 list / create）
 * @param sortOrder  排序序号（升序展示）
 * @param active     是否启用
 */
public record ManagementPermissionView(
        String code,
        ManagementPermissionType type,
        ManagementPermissionScope scope,
        String name,
        String parentCode,
        String resource,
        String action,
        int sortOrder,
        boolean active) {
}
