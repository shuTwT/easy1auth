package com.easy1auth.admin.web;
import com.easy1auth.admin.security.TenantContextFilter; import com.easy1auth.audit.DeliveryService; import com.easy1auth.foundation.web.ApiResponse; import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/webhooks") public class WebhookController {
 private final DeliveryService service; WebhookController(DeliveryService service){this.service=service;}
 @GetMapping ApiResponse<?> list(HttpServletRequest r){return ApiResponse.ok(service.list(c(r).tenantId()));}
 @PostMapping ApiResponse<?> create(HttpServletRequest r,@RequestBody DeliveryService.SubscriptionInput in){return ApiResponse.ok(service.create(c(r).tenantId(),in),"Webhook 创建成功；Secret 仅显示一次");}
 @PutMapping("/{id}") ApiResponse<?> update(HttpServletRequest r,@PathVariable UUID id,@RequestBody DeliveryService.SubscriptionInput in){return ApiResponse.ok(service.update(c(r).tenantId(),id,in),"Webhook 更新成功");}
 @PostMapping("/{id}/rotate-secret") ApiResponse<?> rotate(HttpServletRequest r,@PathVariable UUID id){return ApiResponse.ok(service.rotate(c(r).tenantId(),id),"Webhook Secret 已轮换且仅显示一次");}
 @DeleteMapping("/{id}") ApiResponse<Void> delete(HttpServletRequest r,@PathVariable UUID id){service.delete(c(r).tenantId(),id);return ApiResponse.ok(null,"Webhook 删除成功");}
 private static TenantContext c(HttpServletRequest r){return (TenantContext)Objects.requireNonNull(r.getAttribute(TenantContextFilter.ATTRIBUTE));}
}
