package com.easy1auth.admin.web.dto;

import java.util.List;
import java.util.UUID;

public record UserAccessRoleIds(List<UUID> roleIds) {
    public UserAccessRoleIds {
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
