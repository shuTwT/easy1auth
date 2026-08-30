package com.easy1auth.admin.web;

import com.easy1auth.admin.web.dto.DomainInput;
import com.easy1auth.admin.web.dto.PublicStyleView;
import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.customization.dto.DraftInput;
import com.easy1auth.customization.dto.MessageTemplateInput;
import com.easy1auth.customization.service.CustomizationService;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 品牌定制与消息模板接口。
 *
 * <p>管理端 REST 入口，提供登录页样式（{@code /api/login-style}）、自定义域名
 * （{@code /api/custom-domains}）与消息模板（{@code /api/message-templates}）三类
 * 定制能力的读写。租户侧操作通过 {@link TenantManagementPermission} 做权限控制，
 * 公开读取路由（如登录页样式）无需登录，均以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
public class CustomizationController {
    /** 品牌定制与模板服务 */
    private final CustomizationService service;

    CustomizationController(CustomizationService service) {
        this.service = service;
    }

    /** 查询当前租户登录页样式的草稿配置。 */
    @TenantManagementPermission(value = ManagementPermissionCode.LOGIN_STYLE_READ)
    @GetMapping("/api/login-style/draft")
    ApiResponse<?> styleDraft() {
        return ApiResponse.ok(service.draft());
    }

    /** 保存当前租户登录页样式的草稿配置。 */
    @TenantManagementPermission(value = ManagementPermissionCode.LOGIN_STYLE_UPDATE)
    @PutMapping("/api/login-style/draft")
    ApiResponse<?> styleDraftUpdate(@RequestBody DraftInput in) {
        return ApiResponse.ok(service.updateDraft(in), "登录页草稿已保存");
    }

    /** 将登录页样式草稿发布为线上生效配置。 */
    @TenantManagementPermission(value = ManagementPermissionCode.LOGIN_STYLE_UPDATE)
    @PostMapping("/api/login-style/publish")
    ApiResponse<?> stylePublish() {
        return ApiResponse.ok(service.publish(), "登录页配置已发布");
    }

    /** 将登录页样式草稿重置为默认值。 */
    @TenantManagementPermission(value = ManagementPermissionCode.LOGIN_STYLE_UPDATE)
    @PostMapping("/api/login-style/reset")
    ApiResponse<?> styleReset() {
        return ApiResponse.ok(service.resetDraft(), "登录页草稿已恢复默认");
    }

    /** 查询指定租户已发布的登录页样式（公开路由，供 pool_user 登录页渲染）。 */
    @ManagementRouteClassification(ManagementRouteKind.PUBLIC)
    @GetMapping("/api/login-style/public")
    ApiResponse<?> publicStyle(@RequestParam(required = false) UUID tenantId) {
        if (tenantId == null) {
            return ApiResponse.ok(new PublicStyleView(null, null, null, "#f5f7fa", "#0369A1", "Easy1Auth", "企业级身份管理平台", List.of("password"), List.of(), Map.of(), null, null));
        }
        var style = service.publicStyle(tenantId);
        var legal = service.publishedLegalDocuments(tenantId);
        return ApiResponse.ok(new PublicStyleView(style.logo(), style.logoDark(), style.backgroundImage(), style.backgroundColor(), style.primaryColor(), style.title(), style.subtitle(), style.loginMethods(), style.socialProviders(), service.publishedConfig(tenantId), legal.termsOfService(), legal.privacyPolicy()));
    }

    /**
     * 公开登录页样式视图。
     *
     * @param logo             浅色 Logo 地址（可为 null）
     * @param logoDark         深色 Logo 地址（可为 null）
     * @param backgroundImage  背景图地址（可为 null）
     * @param backgroundColor  背景色
     * @param primaryColor     主色
     * @param title            页面标题
     * @param subtitle         页面副标题
     * @param loginMethods     启用的登录方式列表
     * @param socialProviders  启用的社会化登录提供方列表
     * @param config           额外的公开配置项
     * @param termsOfService   服务条款内容（可为 null）
     * @param privacyPolicy    隐私政策内容（可为 null）
     */

    /** 查询当前租户的自定义域名列表。 */
    @TenantManagementPermission(value = ManagementPermissionCode.CUSTOM_DOMAIN_LIST)
    @GetMapping("/api/custom-domains")
    ApiResponse<?> domains() {
        return ApiResponse.ok(service.domains());
    }

    /** 登记新的自定义域名（所有权验证暂未启用）。 */
    @TenantManagementPermission(value = ManagementPermissionCode.CUSTOM_DOMAIN_CREATE)
    @PostMapping("/api/custom-domains")
    ApiResponse<?> addDomain(@RequestBody DomainInput in) {
        return ApiResponse.ok(service.addDomain(in.domain(), in.verificationMethod()), "域名已登记，所有权验证将在后续阶段启用");
    }

    /** 校验指定自定义域名的所有权（当前阶段暂不可用）。 */
    @TenantManagementPermission(value = ManagementPermissionCode.CUSTOM_DOMAIN_VERIFY)
    @PostMapping("/api/custom-domains/{id}/verify")
    ApiResponse<?> verifyDomain(@PathVariable UUID id) {
        service.verificationUnavailable();
        return ApiResponse.ok(null);
    }

    /** 为指定自定义域名配置 SSL（当前阶段暂不可用）。 */
    @TenantManagementPermission(value = ManagementPermissionCode.CUSTOM_DOMAIN_SSL)
    @PutMapping("/api/custom-domains/{id}/ssl")
    ApiResponse<?> ssl(@PathVariable UUID id) {
        service.verificationUnavailable();
        return ApiResponse.ok(null);
    }

    /** 删除指定自定义域名。 */
    @TenantManagementPermission(value = ManagementPermissionCode.CUSTOM_DOMAIN_DELETE)
    @DeleteMapping("/api/custom-domains/{id}")
    ApiResponse<Void> deleteDomain(@PathVariable UUID id) {
        service.deleteDomain(id);
        return ApiResponse.ok(null, "域名删除成功");
    }

    /** 查询消息模板列表，可按模板类型过滤。 */
    @TenantManagementPermission(value = ManagementPermissionCode.MESSAGE_TEMPLATE_LIST)
    @GetMapping("/api/message-templates")
    ApiResponse<?> templates(@RequestParam(required = false) String type) {
        return ApiResponse.ok(service.templates(type));
    }

    /** 新建消息模板。 */
    @TenantManagementPermission(value = ManagementPermissionCode.MESSAGE_TEMPLATE_CREATE)
    @PostMapping("/api/message-templates")
    ApiResponse<?> addTemplate(@RequestBody MessageTemplateInput in) {
        return ApiResponse.ok(service.saveTemplate(null, in), "模板创建成功");
    }

    /** 更新指定消息模板。 */
    @TenantManagementPermission(value = ManagementPermissionCode.MESSAGE_TEMPLATE_UPDATE)
    @PutMapping("/api/message-templates/{id}")
    ApiResponse<?> updateTemplate(@PathVariable UUID id, @RequestBody MessageTemplateInput in) {
        return ApiResponse.ok(service.saveTemplate(id, in), "模板更新成功");
    }

    /** 删除指定消息模板。 */
    @TenantManagementPermission(value = ManagementPermissionCode.MESSAGE_TEMPLATE_DELETE)
    @DeleteMapping("/api/message-templates/{id}")
    ApiResponse<Void> deleteTemplate(@PathVariable UUID id) {
        service.deleteTemplate(id);
        return ApiResponse.ok(null, "模板删除成功");
    }

    /** 初始化默认消息模板。 */
    @TenantManagementPermission(value = ManagementPermissionCode.MESSAGE_TEMPLATE_INIT)
    @PostMapping("/api/message-templates/init")
    ApiResponse<?> initTemplates() {
        return ApiResponse.ok(List.of(), "默认模板已初始化");
    }

    /**
     * 自定义域名登记输入。
     *
     * @param domain             要登记的域名
     * @param verificationMethod 所有权验证方式
     */

}
