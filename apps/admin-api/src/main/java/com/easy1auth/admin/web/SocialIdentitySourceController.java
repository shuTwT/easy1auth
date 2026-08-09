package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.social.SocialIdentityService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/social-identity-sources")
public class SocialIdentitySourceController {
    private final SocialIdentityService service;

    SocialIdentitySourceController(SocialIdentityService service) {
        this.service = service;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_LIST)
    @GetMapping
    ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String status) {
        var p = service.list(page, pageSize, search, status);
        return ApiResponse.ok(p);
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_STATS)
    @GetMapping("/stats")
    ApiResponse<?> stats() {
        var p = service.list(1, 100, null, null);
        long active = p.items().stream().filter(x -> "active".equals(x.status())).count();
        Map<String, Long> byType = p.items().stream()
                .collect(Collectors.groupingBy(SocialIdentityService.SourceView::type, Collectors.counting()));
        return ApiResponse.ok(new SocialSourceStatsResponse(p.total(), active, p.total() - active, byType));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_READ)
    @GetMapping("/{id}")
    ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_CREATE)
    @PostMapping
    ApiResponse<?> create(@RequestBody SocialIdentityService.Input in) {
        return ApiResponse.ok(service.create(in), "社会化身份源创建成功；Client Secret 仅显示一次");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_UPDATE)
    @PutMapping("/{id}")
    ApiResponse<?> update(@PathVariable UUID id, @RequestBody SocialIdentityService.Input in) {
        return ApiResponse.ok(service.update(id, in), "社会化身份源更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_DELETE)
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "社会化身份源删除成功");
    }

    public record SocialSourceStatsResponse(long totalSources, long activeSources, long inactiveSources,
                                            Map<String, Long> byType) {
    }
}
