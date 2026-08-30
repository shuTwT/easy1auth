package com.easy1auth.admin.web.dto;

import com.easy1auth.adminaccess.dto.ManagementPermissionView;
import com.easy1auth.tenant.dto.TenantPackageView;

import java.util.List;
import java.util.UUID;

public record AuthorizationContextResponse(UUID tenantId, String membershipRole, List<String> permissions,
                                           TenantPackageView tenantPackage, List<ManagementPermissionView> menus) {
}
