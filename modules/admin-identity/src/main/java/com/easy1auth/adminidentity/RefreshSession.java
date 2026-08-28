package com.easy1auth.adminidentity;

import java.util.UUID;

/**
 * 刷新会话轮换结果。
 *
 * @param id               被替换的旧会话 ID
 * @param account          会话所属管理账号
 * @param replacementToken 新签发的替换令牌（明文，仅本次返回）
 */
public record RefreshSession(UUID id, AdminAccount account, String replacementToken) {
}
