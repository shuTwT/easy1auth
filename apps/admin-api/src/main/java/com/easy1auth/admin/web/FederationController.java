package com.easy1auth.admin.web;
import com.easy1auth.admin.security.TenantContextFilter; import com.easy1auth.federation.FederationService; import com.easy1auth.foundation.web.ApiResponse; import com.easy1auth.foundation.web.PageData; import com.easy1auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/social-identity-providers") public class FederationController {
 private final FederationService service; FederationController(FederationService service){this.service=service;}
 @GetMapping ApiResponse<?> list(HttpServletRequest r,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int pageSize,@RequestParam(required=false)String search,@RequestParam(required=false)String status){var p=service.list(c(r).tenantId(),page,pageSize,search,status);return ApiResponse.ok(PageData.of(p.providers(),p.page(),p.pageSize(),p.total()));}
 @GetMapping("/stats") ApiResponse<?> stats(HttpServletRequest r){var p=service.list(c(r).tenantId(),1,100,null,null);long active=p.providers().stream().filter(x->"active".equals(x.status())).count();return ApiResponse.ok(Map.of("totalProviders",p.total(),"activeProviders",active,"inactiveProviders",p.total()-active,"byType",Map.of("oidc",p.total())));}
 @GetMapping("/{id}") ApiResponse<?> get(HttpServletRequest r,@PathVariable UUID id){return ApiResponse.ok(service.get(c(r).tenantId(),id));}
 @PostMapping ApiResponse<?> create(HttpServletRequest r,@RequestBody FederationService.Input in){return ApiResponse.ok(service.create(c(r).tenantId(),in),"OIDC 身份源创建成功；Client Secret 仅显示一次");}
 @PutMapping("/{id}") ApiResponse<?> update(HttpServletRequest r,@PathVariable UUID id,@RequestBody FederationService.Input in){return ApiResponse.ok(service.update(c(r).tenantId(),id,in),"OIDC 身份源更新成功");}
 @DeleteMapping("/{id}") ApiResponse<Void> delete(HttpServletRequest r,@PathVariable UUID id){service.delete(c(r).tenantId(),id);return ApiResponse.ok(null,"OIDC 身份源删除成功");}
 private static TenantContext c(HttpServletRequest r){return (TenantContext)Objects.requireNonNull(r.getAttribute(TenantContextFilter.ATTRIBUTE));}
}
