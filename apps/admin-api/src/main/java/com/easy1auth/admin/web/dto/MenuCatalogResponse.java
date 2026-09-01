package com.easy1auth.admin.web.dto;

import com.easy1auth.system.dto.ManagementPermissionView;

import java.util.List;

public record MenuCatalogResponse(List<ManagementPermissionView> menus, List<ManagementPermissionView> permissions) {
}
