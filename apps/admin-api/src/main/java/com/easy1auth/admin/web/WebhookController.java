package com.easy1auth.admin.web;

import com.easy1auth.admin.annotation.TenantManagementPermission;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.audit.service.DeliveryService;
import com.easy1auth.audit.dto.AuditSubscriptionInput;
import com.easy1auth.common.foundation.web.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Webhook 订阅管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/webhooks}，提供 Webhook 订阅的
 * 查询、创建、更新、签名密钥轮换与删除能力。所有操作均通过
 * {@link TenantManagementPermission} 做租户级权限控制，并以 {@link ApiResponse}
 * 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    /** Webhook 订阅管理服务 */
    private final DeliveryService service;

    WebhookController(DeliveryService service) {
        this.service = service;
    }

    /** 查询当前租户下的 Webhook 订阅列表。 */
    @TenantManagementPermission(value = ManagementPermissionCode.WEBHOOK_LIST)
    @GetMapping
    ApiResponse<?> list() {
        return ApiResponse.ok(service.list());
    }

    /** 创建 Webhook 订阅；返回的 Secret 仅显示一次，请及时保存。 */
    @TenantManagementPermission(value = ManagementPermissionCode.WEBHOOK_CREATE)
    @PostMapping
    ApiResponse<?> create(@RequestBody AuditSubscriptionInput in) {
        return ApiResponse.ok(service.create(in), "Webhook 创建成功；Secret 仅显示一次");
    }

    /** 更新指定 Webhook 订阅的配置。 */
    @TenantManagementPermission(value = ManagementPermissionCode.WEBHOOK_UPDATE)
    @PutMapping("/{id}")
    ApiResponse<?> update(@PathVariable UUID id, @RequestBody AuditSubscriptionInput in) {
        return ApiResponse.ok(service.update(id, in), "Webhook 更新成功");
    }

    /** 轮换指定 Webhook 的签名密钥；新密钥仅显示一次，请及时保存。 */
    @TenantManagementPermission(value = ManagementPermissionCode.WEBHOOK_ROTATE_SECRET)
    @PostMapping("/{id}/rotate-secret")
    ApiResponse<?> rotate(@PathVariable UUID id) {
        return ApiResponse.ok(service.rotate(id), "Webhook Secret 已轮换且仅显示一次");
    }

    /** 删除指定 Webhook 订阅。 */
    @TenantManagementPermission(value = ManagementPermissionCode.WEBHOOK_DELETE)
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "Webhook 删除成功");
    }
}
