package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Embeddable;

/**
 * 租户套餐权限关联的联合主键（{@code @Embeddable} 内嵌主键）。
 *
 * <p>由套餐 ID 与权限编码共同组成，作为 tenant_package_permission 表
 * 复合主键使用。</p>
 */
@Embeddable
public interface TenantPackagePermissionId {
    /** 套餐 ID */
    @Column(name = "package_id")
    long packageId();

    /** 权限编码 */
    @Column(name = "permission_code")
    String permissionCode();
}
