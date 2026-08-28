package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.application.ApplicationService;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 应用管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/applications}，提供应用（OAuth2 客户端）
 * 的分页查询、统计、详情、创建、更新、删除、启停与客户端密钥重新生成能力。
 * 所有操作均通过 {@link TenantManagementPermission} 做租户级权限控制，并以
 * {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    /** 应用服务 */
    private final ApplicationService applications;

    ApplicationController(ApplicationService applications) {
        this.applications = applications;
    }

    /** 分页查询应用列表，支持按名称、类型、状态过滤。 */
    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_LIST)
    @GetMapping
    ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String name, @RequestParam(required = false) String type, @RequestParam(required = false) String status) {
        var p = applications.list(page, pageSize, name, type, status);
        return ApiResponse.ok(p);
    }

    /** 查询应用的统计信息。 */
    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_STATS)
    @GetMapping("/stats")
    ApiResponse<?> stats() {
        return ApiResponse.ok(applications.stats());
    }

    /** 查询指定应用的详情。 */
    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_READ)
    @GetMapping("/{id}")
    ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(applications.get(id));
    }

    /** 创建应用；返回的客户端密钥仅显示一次，请及时保存。 */
    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_CREATE)
    @PostMapping
    ApiResponse<?> create(@RequestBody ApplicationService.ApplicationInput input) {
        return ApiResponse.ok(applications.create(input), "应用创建成功；客户端密钥仅显示一次");
    }

    /** 更新指定应用的配置。 */
    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_UPDATE)
    @PutMapping("/{id}")
    ApiResponse<?> update(@PathVariable UUID id, @RequestBody ApplicationService.ApplicationInput input) {
        return ApiResponse.ok(applications.update(id, input), "应用更新成功");
    }

    /** 删除指定应用。 */
    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_DELETE)
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) {
        applications.delete(id);
        return ApiResponse.ok(null, "应用删除成功");
    }

    /** 更新指定应用的启停状态。 */
    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_STATUS)
    @PutMapping("/{id}/status")
    ApiResponse<?> status(@PathVariable UUID id, @RequestBody StatusInput input) {
        return ApiResponse.ok(applications.status(id, input.status()), "状态更新成功");
    }

    /** 重新生成指定应用的客户端密钥；新密钥仅显示一次，请及时保存。 */
    @TenantManagementPermission(value = ManagementPermissionCode.APPLICATION_REGENERATE_SECRET)
    @PostMapping("/{id}/regenerate-secret")
    ApiResponse<?> regenerate(@PathVariable UUID id) {
        return ApiResponse.ok(applications.regenerateSecret(id), "密钥重新生成成功；新密钥仅显示一次");
    }

    /**
     * 应用启停状态输入。
     *
     * @param status 目标状态：active / disabled
     */
    public record StatusInput(String status) {
    }
}
