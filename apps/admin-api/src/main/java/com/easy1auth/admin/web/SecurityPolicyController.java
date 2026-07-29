package com.easy1auth.admin.web;
import com.easy1auth.admin.security.TenantContextFilter; import com.easy1auth.foundation.web.ApiResponse; import com.easy1auth.security.SecurityPolicyService; import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest; import org.springframework.web.bind.annotation.*; import java.util.Objects;
@RestController @RequestMapping("/api/security-policy") public class SecurityPolicyController {
 private final SecurityPolicyService service; SecurityPolicyController(SecurityPolicyService service){this.service=service;}
 @GetMapping ApiResponse<?> get(HttpServletRequest r){return ApiResponse.ok(service.policy(c(r).tenantId()));}
 @PutMapping ApiResponse<?> update(HttpServletRequest r,@RequestBody SecurityPolicyService.Policy input){return ApiResponse.ok(service.update(c(r).tenantId(),input),"安全策略更新成功");}
 private static TenantContext c(HttpServletRequest r){return (TenantContext)Objects.requireNonNull(r.getAttribute(TenantContextFilter.ATTRIBUTE));}
}
