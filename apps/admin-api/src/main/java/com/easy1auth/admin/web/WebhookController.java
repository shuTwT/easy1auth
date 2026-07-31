package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.audit.DeliveryService;
import com.easy1auth.foundation.web.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    private final DeliveryService service;

    WebhookController(DeliveryService service) {
        this.service = service;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.WEBHOOK_LIST)
    @GetMapping
    ApiResponse<?> list() {
        return ApiResponse.ok(service.list());
    }

    @TenantManagementPermission(value = ManagementPermissionCode.WEBHOOK_CREATE)
    @PostMapping
    ApiResponse<?> create(@RequestBody DeliveryService.SubscriptionInput in) {
        return ApiResponse.ok(service.create(in), "Webhook 创建成功；Secret 仅显示一次");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.WEBHOOK_UPDATE)
    @PutMapping("/{id}")
    ApiResponse<?> update(@PathVariable UUID id, @RequestBody DeliveryService.SubscriptionInput in) {
        return ApiResponse.ok(service.update(id, in), "Webhook 更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.WEBHOOK_ROTATE_SECRET)
    @PostMapping("/{id}/rotate-secret")
    ApiResponse<?> rotate(@PathVariable UUID id) {
        return ApiResponse.ok(service.rotate(id), "Webhook Secret 已轮换且仅显示一次");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.WEBHOOK_DELETE)
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "Webhook 删除成功");
    }
}
