package com.easy1auth.tenant.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Embeddable;

@Embeddable
public interface TenantPackagePermissionId {
    @Column(name = "package_id")
    long packageId();

    @Column(name = "permission_code")
    String permissionCode();
}
