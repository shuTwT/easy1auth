package com.easy1auth.adminaccess.dto;

import java.util.List;
import java.util.UUID;

/**
 * 角色分配结果视图（面向接口层的只读 DTO）。
 *
 * <p>描述给某管理账号在指定租户下分配/移除角色之后的结果。当前授权生效
 * 状态以 "dormant" 占位，表示尚未开启角色授权判定。</p>
 *
 * @param accountId             被分配角色的管理账号 ID
 * @param tenantId              所属租户 ID
 * @param roles                 分配后的角色列表
 * @param authorizationEffective 授权是否已生效（当前恒为 false）
 * @param authorizationStatus    授权状态（当前恒为 "dormant"）
 */
public record AdminRoleAssignmentView(
        UUID accountId,
        UUID tenantId,
        List<AdminRoleView> roles,
        boolean authorizationEffective,
        String authorizationStatus) {
}
