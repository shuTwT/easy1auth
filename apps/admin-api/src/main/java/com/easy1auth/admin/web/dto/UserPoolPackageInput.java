package com.easy1auth.admin.web.dto;

import com.easy1auth.tenant.dto.UserPoolPackageMutation;

import java.util.List;

public record UserPoolPackageInput(String code, String name, int maxUsers, int maxApps, List<String> permissionCodes) {
    public UserPoolPackageMutation toMutation() {
        return new UserPoolPackageMutation(code, name, false, maxUsers, maxApps, permissionCodes);
    }
}
