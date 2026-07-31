package com.easy1auth.admin.web;

import com.easy1auth.tenant.WebFramework;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.application.ApplicationService;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService applications;

    ApplicationController(ApplicationService applications) {
        this.applications = applications;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_LIST)
    @GetMapping
    ApiResponse<?> list(HttpServletRequest request, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String name, @RequestParam(required = false) String type, @RequestParam(required = false) String status) {
        var p = applications.list(context(request).tenantId(), page, pageSize, name, type, status);
        return ApiResponse.ok(PageData.of(p.applications(), p.page(), p.pageSize(), p.total()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_STATS)
    @GetMapping("/stats")
    ApiResponse<?> stats(HttpServletRequest request) {
        return ApiResponse.ok(applications.stats(context(request).tenantId()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_READ)
    @GetMapping("/{id}")
    ApiResponse<?> get(HttpServletRequest request, @PathVariable UUID id) {
        return ApiResponse.ok(applications.get(context(request).tenantId(), id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_CREATE)
    @PostMapping
    ApiResponse<?> create(HttpServletRequest request, @RequestBody ApplicationService.ApplicationInput input) {
        return ApiResponse.ok(applications.create(context(request).tenantId(), input), "应用创建成功；客户端密钥仅显示一次");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_UPDATE)
    @PutMapping("/{id}")
    ApiResponse<?> update(HttpServletRequest request, @PathVariable UUID id, @RequestBody ApplicationService.ApplicationInput input) {
        return ApiResponse.ok(applications.update(context(request).tenantId(), id, input), "应用更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_DELETE)
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(HttpServletRequest request, @PathVariable UUID id) {
        applications.delete(context(request).tenantId(), id);
        return ApiResponse.ok(null, "应用删除成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_STATUS)
    @PutMapping("/{id}/status")
    ApiResponse<?> status(HttpServletRequest request, @PathVariable UUID id, @RequestBody StatusInput input) {
        return ApiResponse.ok(applications.status(context(request).tenantId(), id, input.status()), "状态更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_REGENERATE_SECRET)
    @PostMapping("/{id}/regenerate-secret")
    ApiResponse<?> regenerate(HttpServletRequest request, @PathVariable UUID id) {
        return ApiResponse.ok(applications.regenerateSecret(context(request).tenantId(), id), "密钥重新生成成功；新密钥仅显示一次");
    }

    private static TenantContext context(HttpServletRequest request) {
        return WebFramework.requireTenantContext(request);
    }

    public record StatusInput(String status) {
    }
}
