package com.easy1auth.tenant;

import java.util.Collection;
import java.util.List;

/**
 * 用户池套餐权限目录端口（由权限模块实现）。
 *
 * <p>作为端口将套餐与平台权限体系解耦：用户池模块只依赖本接口，
 * 用于校验用户池套餐可分配的权限编码，以及查询系统套餐可用的平台权限。</p>
 */
public interface UserPoolPackagePermissionCatalog {
    /** 校验并返回用户池套餐可用的权限编码列表（仅允许激活中的用户池权限）。 */
    List<String> validateActiveUserPoolPermissionCodes(Collection<String> permissionCodes);

    /** 返回系统套餐可用的全部平台权限编码。 */
    List<String> activePlatformPermissionCodes();
}
