package com.easy1auth.adminidentity;

import java.time.Instant;
import java.util.UUID;

/**
 * 管理后台账号领域模型（只读 DTO）。
 *
 * <p>表示管理端（admin_user）账号的核心信息，供身份服务与上层接口使用。</p>
 *
 * @param id              账号 ID
 * @param username        用户名
 * @param email           邮箱
 * @param phone           手机号（可为空）
 * @param status          账号状态：active（正常）/ disabled（禁用）
 * @param securityVersion 安全版本号，敏感信息变更时递增
 * @param lastTenantId    最近使用的租户 ID（可为空）
 * @param mfaEnabled      是否启用 MFA
 * @param mfaType         MFA 类型（如 totp）
 * @param lastLoginAt     最近登录时间（可为空）
 * @param createdAt       创建时间
 * @param updatedAt       最后更新时间
 */
public record AdminAccount(UUID id, String username, String email, String phone, String status,
                           long securityVersion, UUID lastTenantId, boolean mfaEnabled,
                           String mfaType, Instant lastLoginAt, Instant createdAt, Instant updatedAt) {
}
