package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.application.ApplicationService;
import com.easy1auth.foundation.web.ApiResponse;
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
    ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String name, @RequestParam(required = false) String type, @RequestParam(required = false) String status) {
        var p = applications.list(page, pageSize, name, type, status);
        return ApiResponse.ok(p);
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_STATS)
    @GetMapping("/stats")
    ApiResponse<?> stats() {
        return ApiResponse.ok(applications.stats());
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_READ)
    @GetMapping("/{id}")
    ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(applications.get(id));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_CREATE)
    @PostMapping
    ApiResponse<?> create(@RequestBody ApplicationService.ApplicationInput input) {
        return ApiResponse.ok(applications.create(input), "应用创建成功；客户端密钥仅显示一次");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_UPDATE)
    @PutMapping("/{id}")
    ApiResponse<?> update(@PathVariable UUID id, @RequestBody ApplicationService.ApplicationInput input) {
        return ApiResponse.ok(applications.update(id, input), "应用更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_DELETE)
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) {
        applications.delete(id);
        return ApiResponse.ok(null, "应用删除成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_STATUS)
    @PutMapping("/{id}/status")
    ApiResponse<?> status(@PathVariable UUID id, @RequestBody StatusInput input) {
        return ApiResponse.ok(applications.status(id, input.status()), "状态更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_REGENERATE_SECRET)
    @PostMapping("/{id}/regenerate-secret")
    ApiResponse<?> regenerate(@PathVariable UUID id) {
        return ApiResponse.ok(applications.regenerateSecret(id), "密钥重新生成成功；新密钥仅显示一次");
    }

    public record StatusInput(String status) {
    }
}
