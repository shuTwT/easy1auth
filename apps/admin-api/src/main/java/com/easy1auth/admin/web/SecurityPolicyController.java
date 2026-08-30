package com.easy1auth.admin.web;

import com.easy1auth.admin.annotation.TenantManagementPermission;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.common.foundation.web.ApiResponse;
import com.easy1auth.security.service.SecurityPolicyService;
import com.easy1auth.security.dto.PolicyView;
import org.springframework.web.bind.annotation.*;

/**
 * 安全策略管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/security-policy}，提供租户安全策略
 * （如密码强度等）的查看与更新能力。操作通过 {@link TenantManagementPermission}
 * 做租户级权限控制，并以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/security-policy")
public class SecurityPolicyController {
    /** 安全策略服务 */
    private final SecurityPolicyService service;

    SecurityPolicyController(SecurityPolicyService service) {
        this.service = service;
    }

    /** 查询当前租户的安全策略配置。 */
    @TenantManagementPermission(value = ManagementPermissionCode.SECURITY_POLICY_READ)
    @GetMapping
    ApiResponse<?> get() {
        return ApiResponse.ok(service.policy());
    }

    /** 更新当前租户的安全策略配置。 */
    @TenantManagementPermission(value = ManagementPermissionCode.SECURITY_POLICY_UPDATE)
    @PutMapping
    ApiResponse<?> update(@RequestBody PolicyView input) {
        return ApiResponse.ok(service.update(input), "安全策略更新成功");
    }
}
