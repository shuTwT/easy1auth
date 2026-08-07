package com.easy1auth.admin.web;

import com.easy1auth.admin.security.*;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.enterpriseidentity.EnterpriseIdentityService;
import com.easy1auth.foundation.web.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/enterprise-identity-sources")
public class EnterpriseIdentitySourceController {
    private final EnterpriseIdentityService service;

    EnterpriseIdentitySourceController(EnterpriseIdentityService service) {
        this.service = service;
    }

    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_LIST)
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String status) {
        var result = service.list(page, pageSize, search, status);
        return ApiResponse.ok(result);
    }

    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_STATS)
    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.ok(service.stats());
    }

    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_READ)
    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_CREATE)
    @PostMapping
    public ApiResponse<?> create(@RequestBody EnterpriseIdentityService.Input input) {
        return ApiResponse.ok(service.create(input), "飞书企业身份源创建成功");
    }

    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable UUID id, @RequestBody EnterpriseIdentityService.Input input) {
        return ApiResponse.ok(service.update(id, input), "飞书企业身份源更新成功");
    }

    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "身份源已删除，导入的数据已转为本地管理");
    }

    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_SYNC)
    @PostMapping("/{id}/sync")
    public ApiResponse<?> sync(@PathVariable UUID id) {
        return ApiResponse.ok(service.sync(id), "同步任务已提交");
    }

    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_READ)
    @GetMapping("/{id}/tasks")
    public ApiResponse<?> tasks(@PathVariable UUID id) {
        return ApiResponse.ok(service.tasks(id));
    }

    @ManagementRouteClassification(ManagementRouteKind.PUBLIC)
    @PostMapping("/{id}/feishu/events")
    public Map<String, Object> feishuEvent(@PathVariable UUID id, @RequestBody Map<String, Object> event) {
        return service.acceptFeishuEvent(id, event);
    }
}
