package com.easy1auth.adminaccess;

import java.util.List;
import java.util.UUID;

public record AdminMembershipView(UUID tenantId, String tenantRole, List<AdminRoleView> roles,
                                  boolean roleAssignmentsEffective, String roleAssignmentStatus) {
}
