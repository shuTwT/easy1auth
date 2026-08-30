package com.easy1auth.admin.web.dto;

import java.util.List;
import java.util.UUID;

public record DirectoryUserIds(List<UUID> userIds) {
    public DirectoryUserIds {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
    }
}
