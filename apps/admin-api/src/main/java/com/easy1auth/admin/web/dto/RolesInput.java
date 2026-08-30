package com.easy1auth.admin.web.dto;

import java.util.List;
import java.util.UUID;

public record RolesInput(UUID tenantId, List<UUID> roleIds) {
}
