package com.easy1auth.admin.web;

import com.easy1auth.tenant.TenantContextHolder;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.federation.FederationService;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/social-identity-providers")
public class FederationController {
    private final FederationService service;

    FederationController(FederationService service) {
        this.service = service;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_PROVIDER_LIST)
    @GetMapping
    ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String status) {
        var p = service.list(TenantContextHolder.requireTenantId(), page, pageSize, search, status);
        return ApiResponse.ok(PageData.of(p.providers(), p.page(), p.pageSize(), p.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_PROVIDER_STATS)
    @GetMapping("/stats")
    ApiResponse<?> stats() {
        var p = service.list(TenantContextHolder.requireTenantId(), 1, 100, null, null);
        long active = p.providers().stream().filter(x -> "active".equals(x.status())).count();
        return ApiResponse.ok(Map.of("totalProviders", p.total(), "activeProviders", active, "inactiveProviders", p.total() - active, "byType", Map.of("oidc", p.total())));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_PROVIDER_READ)
    @GetMapping("/{id}")
    ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(TenantContextHolder.requireTenantId(), id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_PROVIDER_CREATE)
    @PostMapping
    ApiResponse<?> create(@RequestBody FederationService.Input in) {
        return ApiResponse.ok(service.create(TenantContextHolder.requireTenantId(), in), "OIDC 身份源创建成功；Client Secret 仅显示一次");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_PROVIDER_UPDATE)
    @PutMapping("/{id}")
    ApiResponse<?> update(@PathVariable UUID id, @RequestBody FederationService.Input in) {
        return ApiResponse.ok(service.update(TenantContextHolder.requireTenantId(), id, in), "OIDC 身份源更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_PROVIDER_DELETE)
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(TenantContextHolder.requireTenantId(), id);
        return ApiResponse.ok(null, "OIDC 身份源删除成功");
    }
}
