package com.easy1auth.system.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 管理员成员视图（面向接口层的只读 DTO）。
 *
 * <p>描述一个管理账号（admin_user）的完整信息：账号基础资料、在当前选中
 * 用户池内的角色与权限，以及其名下全部用户池的成员关系。角色授权状态当前以
 * "dormant" 占位。</p>
 *
 * @param id                      管理账号 ID
 * @param userPoolId                当前选中用户池 ID
 * @param tenantRole              账号在当前选中用户池内的成员角色
 * @param currentUserPoolId         账号最近一次使用的用户池 ID
 * @param username                登录名
 * @param email                   邮箱
 * @param phone                   手机号
 * @param status                  账号状态：active / disabled
 * @param mfaEnabled              是否已启用 MFA
 * @param mfaType                 MFA 类型（如 totp / email）
 * @param lastLoginAt             最近登录时间
 * @param createdAt               创建时间
 * @param updatedAt               最后更新时间
 * @param roles                   账号在当前选中用户池内被分配的管理角色
 * @param userPools                 账号名下的用户池成员关系列表
 * @param roleAssignmentsEffective 角色分配是否已生效（当前恒为 false）
 * @param roleAssignmentStatus     角色分配状态（当前恒为 "dormant"）
 */
public record AdminMemberView(
        UUID id, UUID userPoolId, String tenantRole, UUID currentUserPoolId, String username,
        String email, String phone, String status, boolean mfaEnabled, String mfaType,
        Instant lastLoginAt, Instant createdAt, Instant updatedAt, List<AdminRoleView> roles,
        List<AdminMembershipView> userPools, boolean roleAssignmentsEffective,
        String roleAssignmentStatus) {
}
