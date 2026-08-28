package com.easy1auth.admin.web;

import com.easy1auth.admin.security.TenantManagementPermission;
import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.infrastructure.foundation.web.ApiResponse;
import com.easy1auth.social.SocialIdentityService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 社会化身份源管理接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/social-identity-sources}，提供社会化
 * 登录身份源（如微信、GitHub 等）的分页查询、统计、详情、创建、更新与删除能力。
 * 所有操作均通过 {@link TenantManagementPermission} 做租户级权限控制，并以
 * {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/social-identity-sources")
public class SocialIdentitySourceController {
    /** 社会化身份服务 */
    private final SocialIdentityService service;

    SocialIdentitySourceController(SocialIdentityService service) {
        this.service = service;
    }

    /** 分页查询社会化身份源列表，支持按名称搜索与状态过滤。 */
    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_LIST)
    @GetMapping
    ApiResponse<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String search, @RequestParam(required = false) String status) {
        var p = service.list(page, pageSize, search, status);
        return ApiResponse.ok(p);
    }

    /** 查询社会化身份源的统计信息（总数、启停数量与按类型分布）。 */
    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_STATS)
    @GetMapping("/stats")
    ApiResponse<?> stats() {
        var p = service.list(1, 100, null, null);
        long active = p.items().stream().filter(x -> "active".equals(x.status())).count();
        Map<String, Long> byType = p.items().stream()
                .collect(Collectors.groupingBy(SocialIdentityService.SourceView::type, Collectors.counting()));
        return ApiResponse.ok(new SocialSourceStatsResponse(p.total(), active, p.total() - active, byType));
    }

    /** 查询指定社会化身份源的详情。 */
    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_READ)
    @GetMapping("/{id}")
    ApiResponse<?> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    /** 创建社会化身份源；返回的 Client Secret 仅显示一次，请及时保存。 */
    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_CREATE)
    @PostMapping
    ApiResponse<?> create(@RequestBody SocialIdentityService.Input in) {
        return ApiResponse.ok(service.create(in), "社会化身份源创建成功；Client Secret 仅显示一次");
    }

    /** 更新指定社会化身份源的配置。 */
    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_UPDATE)
    @PutMapping("/{id}")
    ApiResponse<?> update(@PathVariable UUID id, @RequestBody SocialIdentityService.Input in) {
        return ApiResponse.ok(service.update(id, in), "社会化身份源更新成功");
    }

    /** 删除指定社会化身份源。 */
    @TenantManagementPermission(value = ManagementPermissionCode.SOCIAL_IDENTITY_SOURCE_DELETE)
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null, "社会化身份源删除成功");
    }

    /**
     * 社会化身份源统计响应。
     *
     * @param totalSources    身份源总数
     * @param activeSources   启用的身份源数
     * @param inactiveSources 未启用的身份源数
     * @param byType          按类型分组的数量映射
     */
    public record SocialSourceStatsResponse(long totalSources, long activeSources, long inactiveSources,
                                            Map<String, Long> byType) {
    }
}
