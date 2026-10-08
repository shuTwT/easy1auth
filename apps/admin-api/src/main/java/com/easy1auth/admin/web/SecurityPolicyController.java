package com.easy1auth.admin.web;

import com.easy1auth.admin.annotation.UserPoolManagementPermission;
import com.easy1auth.system.constant.ManagementPermissionCode;
import com.easy1auth.framework.web.response.ApiResponse;
import com.easy1auth.security.service.SecurityPolicyService;
import com.easy1auth.security.dto.PolicyView;
import org.springframework.web.bind.annotation.*;

/**
 * 安全策略管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/security-policy}，提供用户池安全策略
 * （如密码强度等）的查看与更新能力。操作通过 {@link UserPoolManagementPermission}
 * 做用户池级权限控制，并以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/security-policy")
public class SecurityPolicyController {
    /** 安全策略服务 */
    private final SecurityPolicyService service;

    SecurityPolicyController(SecurityPolicyService service) {
        this.service = service;
    }

    /** 查询当前用户池的安全策略配置。 */
    @UserPoolManagementPermission(value = ManagementPermissionCode.SECURITY_POLICY_READ)
    @GetMapping
    ApiResponse<?> get() {
        return ApiResponse.ok(service.policy());
    }

    /** 更新当前用户池的安全策略配置。 */
    @UserPoolManagementPermission(value = ManagementPermissionCode.SECURITY_POLICY_UPDATE)
    @PutMapping
    ApiResponse<?> update(@RequestBody PolicyView input) {
        return ApiResponse.ok(service.update(input), "安全策略更新成功");
    }
}
