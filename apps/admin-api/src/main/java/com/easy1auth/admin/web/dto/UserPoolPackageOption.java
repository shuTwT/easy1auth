package com.easy1auth.admin.web.dto;

import com.easy1auth.tenant.dto.UserPoolPackageView;

public record UserPoolPackageOption(long id, String name, int maxUsers, int maxApps) {
    public static UserPoolPackageOption from(UserPoolPackageView item) {
        return new UserPoolPackageOption(item.id(), item.name(), item.maxUsers(), item.maxApps());
    }
}
