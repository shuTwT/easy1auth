package com.easy1auth.adminaccess;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminMemberView(
        UUID id, UUID tenantId, String tenantRole, UUID currentTenantId, String username,
        String email, String phone, String status, boolean mfaEnabled, String mfaType,
        Instant lastLoginAt, Instant createdAt, Instant updatedAt, List<AdminRoleView> roles,
        List<AdminMembershipView> tenants, boolean roleAssignmentsEffective,
        String roleAssignmentStatus) {
}
