package com.easy1auth.adminaccess;

import java.util.List;
import java.util.UUID;

public record AdminRoleAssignmentView(
        UUID accountId,
        UUID tenantId,
        List<AdminRoleView> roles,
        boolean authorizationEffective,
        String authorizationStatus) {
}
