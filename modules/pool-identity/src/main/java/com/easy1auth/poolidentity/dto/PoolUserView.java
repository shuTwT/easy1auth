package com.easy1auth.poolidentity.dto;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * pool_user 用户视图（面向接口层的只读 DTO）。
 *
 * <p>用于目录列表、详情等展示场景，描述一个第三方接入用户的完整档案信息。
 * pool_user 属于租户隔离的用户体系，供第三方授权登录使用，不在系统后台登录。</p>
 *
 * @param id               用户 ID
 * @param tenantId         所属租户 ID
 * @param username         登录用户名（唯一，用于授权登录）
 * @param email            邮箱（可为 null）
 * @param phone            手机号
 * @param name             用户姓名/显示名
 * @param avatar           头像地址
 * @param status           用户状态：active（正常）/ disabled（禁用）/ locked（锁定）
 * @param emailVerified    邮箱是否已验证
 * @param phoneVerified    手机号是否已验证
 * @param department       所属部门
 * @param position         岗位名称
 * @param customAttributes 自定义扩展属性（键值对，可为 null）
 * @param lastLoginAt      最近一次登录时间（可为 null，表示从未登录）
 * @param createdAt        创建时间
 * @param updatedAt        最后更新时间
 */
public record PoolUserView(UUID id, UUID tenantId, String username, @Nullable String email, String phone, String name,
                           String avatar, String status, boolean emailVerified, boolean phoneVerified,
                           String department, String position, Map<String, Object> customAttributes,
                           Instant lastLoginAt, Instant createdAt, Instant updatedAt) {
}
