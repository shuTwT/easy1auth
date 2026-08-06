package com.easy1auth.admin.web;

import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.customization.*;
import com.easy1auth.foundation.web.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class CustomizationController {
    private final CustomizationService service;

    CustomizationController(CustomizationService service) {
        this.service = service;
    }

    @TenantManagementPermission(value = ManagementPermissionCode.BRAND_READ)
    @GetMapping("/api/brand-settings")
    ApiResponse<?> brand() {
        var e = service.brand();
        return ApiResponse.ok(Map.of("tenantId", e.tenantId(), "brandSettings", e.settings(), "updatedAt", e.updatedAt()));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.BRAND_UPDATE)
    @PutMapping("/api/brand-settings")
    ApiResponse<?> brandUpdate(@RequestBody Map<String, Object> in) {
        var settings = in.containsKey("brandSettings") ? (Map<String, Object>) in.get("brandSettings") : in;
        var e = service.updateBrand(settings);
        return ApiResponse.ok(Map.of("tenantId", e.tenantId(), "brandSettings", e.settings(), "updatedAt", e.updatedAt()), "品牌设置更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.LOGIN_STYLE_READ)
    @GetMapping("/api/login-style")
    ApiResponse<?> style() {
        return ApiResponse.ok(service.style());
    }

    @TenantManagementPermission(value = ManagementPermissionCode.LOGIN_STYLE_UPDATE)
    @PutMapping("/api/login-style")
    ApiResponse<?> styleUpdate(@RequestBody CustomizationService.StyleInput in) {
        return ApiResponse.ok(service.updateStyle(in), "登录样式更新成功");
    }

    @ManagementRouteClassification(ManagementRouteKind.PUBLIC)
    @GetMapping("/api/login-style/public")
    ApiResponse<?> publicStyle(@RequestParam(required = false) UUID tenantId) {
        if (tenantId == null)
            return ApiResponse.ok(new PublicStyleView(null, null, null, "#f5f7fa", "#0369A1", "Easy1Auth", "企业级身份管理平台", List.of("password"), List.of()));
        var style = service.publicStyle(tenantId);
        return ApiResponse.ok(new PublicStyleView(style.logo(), style.logoDark(), style.backgroundImage(), style.backgroundColor(), style.primaryColor(), style.title(), style.subtitle(), style.loginMethods(), style.socialProviders()));
    }

    record PublicStyleView(String logo, String logoDark, String backgroundImage, String backgroundColor,
                           String primaryColor, String title, String subtitle, List<String> loginMethods,
                           List<String> socialProviders) { }

    @TenantManagementPermission(value = ManagementPermissionCode.CUSTOM_DOMAIN_LIST)
    @GetMapping("/api/custom-domains")
    ApiResponse<?> domains() {
        return ApiResponse.ok(service.domains());
    }

    @TenantManagementPermission(value = ManagementPermissionCode.CUSTOM_DOMAIN_CREATE)
    @PostMapping("/api/custom-domains")
    ApiResponse<?> addDomain(@RequestBody DomainInput in) {
        return ApiResponse.ok(service.addDomain(in.domain(), in.verificationMethod()), "域名已登记，所有权验证将在后续阶段启用");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.CUSTOM_DOMAIN_VERIFY)
    @PostMapping("/api/custom-domains/{id}/verify")
    ApiResponse<?> verifyDomain(@PathVariable UUID id) {
        service.verificationUnavailable();
        return ApiResponse.ok(null);
    }

    @TenantManagementPermission(value = ManagementPermissionCode.CUSTOM_DOMAIN_SSL)
    @PutMapping("/api/custom-domains/{id}/ssl")
    ApiResponse<?> ssl(@PathVariable UUID id) {
        service.verificationUnavailable();
        return ApiResponse.ok(null);
    }

    @TenantManagementPermission(value = ManagementPermissionCode.CUSTOM_DOMAIN_DELETE)
    @DeleteMapping("/api/custom-domains/{id}")
    ApiResponse<Void> deleteDomain(@PathVariable UUID id) {
        service.deleteDomain(id);
        return ApiResponse.ok(null, "域名删除成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.MESSAGE_TEMPLATE_LIST)
    @GetMapping("/api/message-templates")
    ApiResponse<?> templates(@RequestParam(required = false) String type) {
        return ApiResponse.ok(service.templates(type));
    }

    @TenantManagementPermission(value = ManagementPermissionCode.MESSAGE_TEMPLATE_CREATE)
    @PostMapping("/api/message-templates")
    ApiResponse<?> addTemplate(@RequestBody CustomizationService.TemplateInput in) {
        return ApiResponse.ok(service.saveTemplate(null, in), "模板创建成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.MESSAGE_TEMPLATE_UPDATE)
    @PutMapping("/api/message-templates/{id}")
    ApiResponse<?> updateTemplate(@PathVariable UUID id, @RequestBody CustomizationService.TemplateInput in) {
        return ApiResponse.ok(service.saveTemplate(id, in), "模板更新成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.MESSAGE_TEMPLATE_DELETE)
    @DeleteMapping("/api/message-templates/{id}")
    ApiResponse<Void> deleteTemplate(@PathVariable UUID id) {
        service.deleteTemplate(id);
        return ApiResponse.ok(null, "模板删除成功");
    }

    @TenantManagementPermission(value = ManagementPermissionCode.MESSAGE_TEMPLATE_INIT)
    @PostMapping("/api/message-templates/init")
    ApiResponse<?> initTemplates() {
        return ApiResponse.ok(List.of(), "默认模板已初始化");
    }

    public record DomainInput(String domain, String verificationMethod) {
    }
}
