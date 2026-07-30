package com.easy1auth.tenant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record TenantPackageMutation(
        String code,
        String name,
        boolean defaultPackage,
        int maxUsers,
        int maxApps,
        List<String> permissionCodes
) {
    public TenantPackageMutation {
        permissionCodes = permissionCodes == null ? null : Collections.unmodifiableList(new ArrayList<>(permissionCodes));
    }
}
