package com.easy1auth.adminaccess;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 管理员角色视图（面向接口层的只读 DTO）。
 *
 * <p>描述租户内一个管理角色（admin_role）的基本信息、所拥有的管理端权限码，
 * 以及当前关联的管理员数量。</p>
 *
 * @param id          角色 ID
 * @param tenantId    所属租户 ID
 * @param name        角色名称
 * @param description 角色描述（可为 null）
 * @param permissions 角色拥有的管理端权限码列表
 * @param isSystem    是否系统预置角色（系统角色不可修改/删除）
 * @param adminCount  关联的管理员数量
 * @param createdAt   创建时间
 * @param updatedAt   最后更新时间
 */
public record AdminRoleView(UUID id, UUID tenantId, String name, String description, List<String> permissions,
                            boolean isSystem, long adminCount, Instant createdAt, Instant updatedAt) {
}
