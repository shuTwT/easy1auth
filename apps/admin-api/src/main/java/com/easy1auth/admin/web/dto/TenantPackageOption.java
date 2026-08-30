package com.easy1auth.admin.web.dto;

import com.easy1auth.tenant.dto.TenantPackageView;

public record TenantPackageOption(long id, String name, int maxUsers, int maxApps) {
    public static TenantPackageOption from(TenantPackageView item) {
        return new TenantPackageOption(item.id(), item.name(), item.maxUsers(), item.maxApps());
    }
}
