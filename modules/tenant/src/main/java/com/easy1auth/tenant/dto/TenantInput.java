package com.easy1auth.tenant.dto;

/** 租户基础资料；所属用户池从已授权的管理上下文获取，禁止从请求体指定。 */
public record TenantInput(String name, String code, String description, String status) {
}
