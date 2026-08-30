package com.easy1auth.admin.web.dto;

import java.util.List;
import java.util.UUID;

public record UserAccessUserIds(List<UUID> userIds) {
    public UserAccessUserIds {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
    }
}
