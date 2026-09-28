package com.easy1auth.admin.web.dto;

import com.easy1auth.system.dto.ManagementPermissionView;
import java.util.List;

public record PermissionCatalogResponse(List<ManagementPermissionView> permissions) {
}
