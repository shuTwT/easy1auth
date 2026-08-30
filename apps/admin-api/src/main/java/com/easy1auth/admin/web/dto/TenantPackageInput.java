package com.easy1auth.admin.web.dto;

import com.easy1auth.tenant.dto.TenantPackageMutation;

import java.util.List;

public record TenantPackageInput(String code, String name, int maxUsers, int maxApps, List<String> permissionCodes) {
    public TenantPackageMutation toMutation() {
        return new TenantPackageMutation(code, name, false, maxUsers, maxApps, permissionCodes);
    }
}
