package com.easy1auth.adminaccess.dto;

import java.util.List;
import java.util.UUID;

/**
 * 管理账号在某租户内的成员关系视图（面向接口层的只读 DTO）。
 *
 * <p>描述一个管理账号在某个租户下的成员角色，以及该账号被分配的租户内
 * 管理角色（对应 {@link AdminRoleView}）。当前角色分配生效状态以
 * "dormant" 占位，表示尚未开启角色授权判定。</p>
 *
 * @param tenantId                所属租户 ID
 * @param tenantRole              账号在该租户内的成员角色（如 owner / tenant_admin / common）
 * @param roles                   账号在该租户内被分配的管理角色列表
 * @param roleAssignmentsEffective 角色分配是否已生效（当前恒为 false）
 * @param roleAssignmentStatus     角色分配状态（当前恒为 "dormant"）
 */
public record AdminMembershipView(UUID tenantId, String tenantRole, List<AdminRoleView> roles,
                                  boolean roleAssignmentsEffective, String roleAssignmentStatus) {
}
