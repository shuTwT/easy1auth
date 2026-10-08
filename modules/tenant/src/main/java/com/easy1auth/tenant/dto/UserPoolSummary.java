package com.easy1auth.tenant.dto;

import java.util.UUID;

/**
 * 用户池摘要视图（面向接口层的只读 DTO）。
 *
 * <p>用于用户池列表、切换用户池等场景，描述一个用户池的关键信息以及当前账号
 * 在该用户池中的角色。</p>
 *
 * @param id           用户池 ID
 * @param name         用户池名称
 * @param status       用户池状态：active / suspended / deleted
 * @param system       是否系统用户池
 * @param userPoolPackage 绑定的用户池套餐摘要
 * @param role         当前账号在该用户池内的角色：super_admin / tenant_admin / common
 */
public record UserPoolSummary(UUID id, String name, String status, boolean system, UserPoolPackageView userPoolPackage,
                            String role) {
}
