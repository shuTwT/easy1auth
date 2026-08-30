package com.easy1auth.admin.web.dto;

import java.util.UUID;

public record LoginUser(UUID id, String username, String email, String avatar, UUID currentTenantId) {
}
