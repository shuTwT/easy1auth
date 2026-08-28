package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.time.Instant;

/**
 * 租户套餐权限关联实体（对应 tenant_package_permission 表）。
 *
 * <p>套餐与平台权限的多对多关联表，联合主键为（套餐 ID, 权限编码），
 * 表示某个套餐允许授予租户的权限项。</p>
 */
@Entity
@Table(name = "tenant_package_permission")
public interface TenantPackagePermissionEntity {
    /** 联合主键：套餐 ID + 权限编码 */
    @Id
    TenantPackagePermissionId id();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();
}
