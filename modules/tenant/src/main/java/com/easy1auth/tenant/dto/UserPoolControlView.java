package com.easy1auth.tenant.dto;

import java.util.UUID;

/**
 * 用户池控制视图（面向平台管理的只读 DTO）。
 *
 * <p>用于平台侧用户池管理列表与详情，在用户池基本信息之上额外携带
 * 当前管理员账号 ID，便于平台展示与执行管理操作。</p>
 *
 * @param id                     用户池 ID
 * @param name                   用户池名称
 * @param status                 用户池状态：active / suspended / deleted
 * @param userPoolPackage          用户池绑定的套餐摘要
 * @param administratorAccountId 当前激活的管理员账号 ID（无管理员时为 null）
 */
public record UserPoolControlView(
        UUID id,
        String name,
        String status,
        UserPoolPackageView userPoolPackage,
        UUID administratorAccountId) {
}
