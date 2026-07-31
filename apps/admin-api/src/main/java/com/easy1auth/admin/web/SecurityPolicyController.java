package com.easy1auth.admin.web;

import com.easy1auth.tenant.TenantContextHolder;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.security.SecurityPolicyService;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/security-policy")
public class SecurityPolicyController {
    private final SecurityPolicyService service;

    SecurityPolicyController(SecurityPolicyService service) {
        this.service = service;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SECURITY_POLICY_READ)
    @GetMapping
    ApiResponse<?> get() {
        return ApiResponse.ok(service.policy(TenantContextHolder.requireTenantId()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.SECURITY_POLICY_UPDATE)
    @PutMapping
    ApiResponse<?> update(@RequestBody SecurityPolicyService.Policy input) {
        return ApiResponse.ok(service.update(TenantContextHolder.requireTenantId(), input), "安全策略更新成功");
    }
}
