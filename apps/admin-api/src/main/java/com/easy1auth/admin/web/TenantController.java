package com.easy1auth.admin.web;

import com.easy1auth.foundation.trace.TraceIdFilter;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantService tenants;
    TenantController(TenantService tenants) { this.tenants = tenants; }

    @GetMapping("/list")
    public Map<String, Object> list(Principal principal) {
        var rows = tenants.list(accountId(principal));
        return Map.of("status", "success", "tenants", rows, "total", rows.size());
    }

    @PostMapping("/create")
    public Map<String, Object> create(Principal principal, @RequestBody CreateTenant request) {
        var tenant = tenants.create(accountId(principal), request.name(), request.plan());
        return Map.of("status", "success", "message", "租户创建成功", "tenant", tenant);
    }

    @GetMapping("/current")
    public Map<String, Object> current(Principal principal, @RequestHeader("tenant-id") UUID tenantId,
                                       HttpServletRequest request) {
        var context = tenants.resolve(accountId(principal), tenantId, traceId(request));
        var tenant = tenants.list(context.accountId()).stream().filter(it -> it.id().equals(tenantId)).findFirst().orElseThrow();
        return Map.of("status", "success", "tenant", tenant);
    }

    @PostMapping("/{tenantId}/owner-transfer")
    public ApiResponse<Void> transfer(Principal principal, @PathVariable UUID tenantId, @RequestBody TransferOwner request) {
        tenants.transferOwner(accountId(principal), tenantId, request.targetAccountId());
        return ApiResponse.ok(null, "租户所有权转移成功");
    }

    @DeleteMapping("/{tenantId}/members/{accountId}")
    public ApiResponse<Void> remove(Principal principal, @PathVariable UUID tenantId, @PathVariable UUID accountId) {
        tenants.removeMember(accountId(principal), tenantId, accountId);
        return ApiResponse.ok(null, "成员移除成功");
    }

    private static UUID accountId(Principal principal) { return UUID.fromString(principal.getName()); }
    private static String traceId(HttpServletRequest request) { return request.getHeader(TraceIdFilter.HEADER); }
    public record CreateTenant(String name, String plan) {}
    public record TransferOwner(UUID targetAccountId) {}
}
