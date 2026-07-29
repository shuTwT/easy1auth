package com.easy1auth.admin.web;
import com.easy1auth.admin.security.TenantContextFilter; import com.easy1auth.customization.*; import com.easy1auth.foundation.web.ApiResponse; import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController public class CustomizationController {
 private final CustomizationService service; CustomizationController(CustomizationService service){this.service=service;}
 @GetMapping("/api/brand-settings") ApiResponse<?> brand(HttpServletRequest r){var e=service.brand(c(r).tenantId());return ApiResponse.ok(Map.of("tenantId",e.tenantId(),"brandSettings",e.settings(),"updatedAt",e.updatedAt()));}
 @PutMapping("/api/brand-settings") ApiResponse<?> brandUpdate(HttpServletRequest r,@RequestBody Map<String,Object> in){var settings=in.containsKey("brandSettings")?(Map<String,Object>)in.get("brandSettings"):in;var e=service.updateBrand(c(r).tenantId(),settings);return ApiResponse.ok(Map.of("tenantId",e.tenantId(),"brandSettings",e.settings(),"updatedAt",e.updatedAt()),"品牌设置更新成功");}
 @GetMapping("/api/brand-settings/public") ApiResponse<?> publicBrand(@RequestParam(required=false)UUID tenantId){return ApiResponse.ok(tenantId==null?Map.of():service.brand(tenantId).settings());}
 @GetMapping("/api/login-style") ApiResponse<?> style(HttpServletRequest r){return ApiResponse.ok(service.style(c(r).tenantId()));}
 @PutMapping("/api/login-style") ApiResponse<?> styleUpdate(HttpServletRequest r,@RequestBody CustomizationService.StyleInput in){return ApiResponse.ok(service.updateStyle(c(r).tenantId(),in),"登录样式更新成功");}
 @GetMapping("/api/login-style/public") ApiResponse<?> publicStyle(@RequestParam(required=false)UUID tenantId){return ApiResponse.ok(tenantId==null?Map.of("backgroundColor","#f5f7fa","primaryColor","#0369A1","title","Easy1Auth","subtitle","企业级身份管理平台","loginMethods",List.of("password"),"socialProviders",List.of()):service.style(tenantId));}
 @GetMapping("/api/custom-domains") ApiResponse<?> domains(HttpServletRequest r){return ApiResponse.ok(service.domains(c(r).tenantId()));}
 @PostMapping("/api/custom-domains") ApiResponse<?> addDomain(HttpServletRequest r,@RequestBody DomainInput in){return ApiResponse.ok(service.addDomain(c(r).tenantId(),in.domain(),in.verificationMethod()),"域名已登记，所有权验证将在后续阶段启用");}
 @PostMapping("/api/custom-domains/{id}/verify") ApiResponse<?> verifyDomain(@PathVariable UUID id){service.verificationUnavailable();return ApiResponse.ok(null);}
 @PutMapping("/api/custom-domains/{id}/ssl") ApiResponse<?> ssl(@PathVariable UUID id){service.verificationUnavailable();return ApiResponse.ok(null);}
 @DeleteMapping("/api/custom-domains/{id}") ApiResponse<Void> deleteDomain(HttpServletRequest r,@PathVariable UUID id){service.deleteDomain(c(r).tenantId(),id);return ApiResponse.ok(null,"域名删除成功");}
 @GetMapping("/api/message-templates") ApiResponse<?> templates(HttpServletRequest r,@RequestParam(required=false)String type){return ApiResponse.ok(service.templates(c(r).tenantId(),type));}
 @PostMapping("/api/message-templates") ApiResponse<?> addTemplate(HttpServletRequest r,@RequestBody CustomizationService.TemplateInput in){return ApiResponse.ok(service.saveTemplate(c(r).tenantId(),null,in),"模板创建成功");}
 @PutMapping("/api/message-templates/{id}") ApiResponse<?> updateTemplate(HttpServletRequest r,@PathVariable UUID id,@RequestBody CustomizationService.TemplateInput in){return ApiResponse.ok(service.saveTemplate(c(r).tenantId(),id,in),"模板更新成功");}
 @DeleteMapping("/api/message-templates/{id}") ApiResponse<Void> deleteTemplate(HttpServletRequest r,@PathVariable UUID id){service.deleteTemplate(c(r).tenantId(),id);return ApiResponse.ok(null,"模板删除成功");}
 @PostMapping("/api/message-templates/init") ApiResponse<?> initTemplates(){return ApiResponse.ok(List.of(),"默认模板已初始化");}
 private static TenantContext c(HttpServletRequest r){return (TenantContext)Objects.requireNonNull(r.getAttribute(TenantContextFilter.ATTRIBUTE));}
 public record DomainInput(String domain,String verificationMethod){}
}
