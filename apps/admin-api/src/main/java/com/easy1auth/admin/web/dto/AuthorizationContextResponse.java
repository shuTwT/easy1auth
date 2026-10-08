package com.easy1auth.admin.web.dto;

import com.easy1auth.system.dto.ManagementPermissionView;
import com.easy1auth.tenant.dto.UserPoolPackageView;

import java.util.List;
import java.util.UUID;

public record AuthorizationContextResponse(UUID userPoolId, String membershipRole, List<String> permissions,
                                           UserPoolPackageView userPoolPackage, List<ManagementPermissionView> menus) {
}
