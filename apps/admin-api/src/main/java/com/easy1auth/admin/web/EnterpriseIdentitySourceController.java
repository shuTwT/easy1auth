package com.easy1auth.admin.web;

import com.easy1auth.admin.security.*;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.enterpriseidentity.EnterpriseIdentityService;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 飞书企业身份源管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/enterprise-identity-sources}，提供飞书
 * 企业身份源的查询、统计、创建、更新、删除与同步能力，并接收飞书平台回推的
 * 事件回调。除回调外的操作均通过 {@link TenantManagementPermission} 做租户级
 * 权限控制，并以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/enterprise-identity-sources")
public class EnterpriseIdentitySourceController {
    /** 飞书企业身份源服务 */
    private final EnterpriseIdentityService service;
    /** JSON 解析器（用于把回调负载转换为事件对象） */
    private final ObjectMapper json;

    EnterpriseIdentitySourceController(EnterpriseIdentityService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    /** 分页查询企业身份源列表，支持按名称搜索与状态过滤。 */
    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_LIST)
    @GetMapping
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String status) {
        var result = service.list(page, pageSize, search, status);
        return ApiResponse.ok(result);
    }

    /** 查询企业身份源的整体统计信息。 */
    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_STATS)
    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.ok(service.stats());
    }

    /** 查询指定企业身份源的详情。 */
    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_READ)
    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    /** 创建飞书企业身份源。 */
    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_CREATE)
    @PostMapping
    public ApiResponse<?> create(@RequestBody EnterpriseIdentityService.Input input) {
        return ApiResponse.ok(service.create(input), "飞书企业身份源创建成功");
    }

    /** 更新指定飞书企业身份源的配置。 */
    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable UUID id, @RequestBody EnterpriseIdentityService.Input input) {
        return ApiResponse.ok(service.update(id, input), "飞书企业身份源更新成功");
    }

    /** 删除指定企业身份源；已导入的用户数据转为本地管理。 */
    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "身份源已删除，导入的数据已转为本地管理");
    }

    /** 提交一次企业身份源的同步任务。 */
    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_SYNC)
    @PostMapping("/{id}/sync")
    public ApiResponse<?> sync(@PathVariable UUID id) {
        return ApiResponse.ok(service.sync(id), "同步任务已提交");
    }

    /** 查询指定企业身份源的同步任务历史。 */
    @TenantManagementPermission(ManagementPermissionCode.ENTERPRISE_IDENTITY_SOURCE_READ)
    @GetMapping("/{id}/tasks")
    public ApiResponse<?> tasks(@PathVariable UUID id) {
        return ApiResponse.ok(service.tasks(id));
    }

    /** 接收飞书平台推送的事件回调（公开路由，无需登录）。 */
    @ManagementRouteClassification(ManagementRouteKind.PUBLIC)
    @PostMapping("/{id}/feishu/events")
    public EnterpriseIdentityService.FeishuEventResponse feishuEvent(@PathVariable UUID id, @RequestBody FeishuEventRequest event) {
        return service.acceptFeishuEvent(id, json.convertValue(event.fields, new TypeReference<>() {
        }));
    }

    /** 飞书事件回调的原始请求体（字段不定，用任意 setter 收集全部字段）。 */
    public static final class FeishuEventRequest {
        private final Map<String, Object> fields = new LinkedHashMap<>();

        @JsonAnySetter
        public void set(String name, Object value) {
            fields.put(name, value);
        }
    }
}
