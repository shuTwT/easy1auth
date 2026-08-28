package com.easy1auth.admin.security;

import com.easy1auth.adminaccess.ManagementPermissionCode;
import com.easy1auth.adminaccess.ManagementPermissionScope;
import com.easy1auth.adminaccess.ManagementPermissionType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 管理路由清单与权限元数据仓库。
 *
 * <p>应用启动时扫描 {@code com.easy1auth.admin.web} 包下全部控制器路由，解析每个路由
 * 声明的权限注解或路由分类并构建内部索引；运行期按请求路径与方法匹配路由，供
 * {@link TenantContextFilter} 与 {@link TenantSecurityFilter} 判断是否需要租户上下文、
 * 要求何种租户权限。</p>
 */
@Component
public final class ManagementRouteInventory implements SmartInitializingSingleton {
    /** 管理控制器所在包，仅扫描该包下的路由 */
    private static final String CONTROLLER_PACKAGE = "com.easy1auth.admin.web";
    /** 显式声明的公开路由白名单（方法 + 路径） */
    private static final Set<String> PUBLIC_ROUTES = Set.of("GET /api/login-style/public", "POST /api/enterprise-identity-sources/{id}/feishu/events");

    /** Spring MVC 的请求映射注册表，用于发现全部处理器方法 */
    private final RequestMappingHandlerMapping mappings;
    /** 构建完成后冻结的路由清单（volatile 保证可见性） */
    private volatile List<Route> routes = List.of();

    ManagementRouteInventory(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings) {
        this.mappings = mappings;
    }

    /** 应用启动完成后扫描管理控制器路由并构建路由索引，无路由时抛异常拒绝启动。 */
    @Override
    public void afterSingletonsInstantiated() {
        List<Route> discovered = new ArrayList<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().getPackageName().equals(CONTROLLER_PACKAGE)) {
                return;
            }
            RouteMetadata metadata = metadata(handler, mapping);
            for (PathPattern pattern : mapping.getPathPatternsCondition().getPatterns()) {
                discovered.add(new Route(pattern, mapping.getMethodsCondition().getMethods(), metadata));
            }
        });
        if (discovered.isEmpty()) {
            throw new IllegalStateException("No management controller routes were discovered");
        }
        routes = List.copyOf(discovered);
    }

    /** 判断当前请求是否要求先建立租户上下文（租户权限路由或授权上下文路由）。 */
    public boolean requiresTenantContext(HttpServletRequest request) {
        return metadata(request).map(RouteMetadata::requiresTenantContext).orElse(false);
    }

    /** 返回当前请求要求的租户权限编码；路由无租户权限要求时返回空。 */
    public Optional<ManagementPermissionCode> tenantPermission(HttpServletRequest request) {
        return metadata(request)
                .map(RouteMetadata::tenant)
                .map(TenantManagementPermission::value);
    }

    /** 按请求路径与方法匹配内部路由索引，返回对应的路由元数据。 */
    private Optional<RouteMetadata> metadata(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        return routes.stream()
                .filter(route -> route.matches(path, method))
                .map(Route::metadata)
                .findFirst();
    }

    /**
     * 提取处理器方法的权限元数据。
     *
     * <p>一个方法必须在租户权限、平台权限、路由分类三者中恰好声明一种，否则视为配置
     * 错误；同时按声明类型做对应的一致性校验。</p>
     */
    private static RouteMetadata metadata(HandlerMethod handler, RequestMappingInfo mapping) {
        Method method = handler.getMethod();
        TenantManagementPermission tenant = method.getAnnotation(TenantManagementPermission.class);
        PlatformManagementPermission platform = method.getAnnotation(PlatformManagementPermission.class);
        ManagementRouteClassification classification = method.getAnnotation(ManagementRouteClassification.class);
        int declarations = (tenant == null ? 0 : 1) + (platform == null ? 0 : 1) + (classification == null ? 0 : 1);
        if (declarations != 1) {
            throw invalid(handler, "must declare exactly one tenant requirement, platform requirement, or explicit classification");
        }
        if (tenant != null) {
            validateTenant(handler, tenant);
            return new RouteMetadata(tenant, null, null);
        }
        if (platform != null) {
            validatePlatform(handler, platform);
            return new RouteMetadata(null, platform, null);
        }
        validateClassification(handler, mapping, classification.value());
        return new RouteMetadata(null, null, classification.value());
    }

    /** 校验租户权限编码：scope 必须为 TENANT，且类型为 ACTION 或 MENU。 */
    private static void validateTenant(HandlerMethod handler, TenantManagementPermission requirement) {
        ManagementPermissionCode code = requirement.value();
        if (code.scope() != ManagementPermissionScope.TENANT
                || (code.type() != ManagementPermissionType.ACTION && code.type() != ManagementPermissionType.MENU)) {
            throw invalid(handler, "has an invalid tenant permission");
        }
    }

    /** 校验平台权限编码：scope 必须为 PLATFORM 且类型为 ACTION。 */
    private static void validatePlatform(HandlerMethod handler, PlatformManagementPermission requirement) {
        ManagementPermissionCode code = requirement.value();
        if (code.scope() != ManagementPermissionScope.PLATFORM || code.type() != ManagementPermissionType.ACTION) {
            throw invalid(handler, "has an invalid platform permission");
        }
    }

    /** 校验显式路由分类与其归属控制器、公开路由白名单是否一致。 */
    private static void validateClassification(HandlerMethod handler, RequestMappingInfo mapping, ManagementRouteKind kind) {
        String controller = handler.getBeanType().getSimpleName();
        if (kind == ManagementRouteKind.PUBLIC && !routeKeys(mapping).stream().allMatch(PUBLIC_ROUTES::contains)) {
            throw invalid(handler, "is not an allowlisted public route");
        }
        if (kind == ManagementRouteKind.AUTHENTICATION && !"AuthController".equals(controller)) {
            throw invalid(handler, "is not an authentication controller route");
        }
        if (kind == ManagementRouteKind.AUTHENTICATED_SELF && !"AdminSecurityController".equals(controller)) {
            throw invalid(handler, "is not an authenticated self-service route");
        }
        if (kind == ManagementRouteKind.AUTHORIZATION_CONTEXT && !"AuthorizationContextController".equals(controller)) {
            throw invalid(handler, "is not the authorization context route");
        }
        if (kind == ManagementRouteKind.DEFERRED_TODO_7 && !"TenantController".equals(controller)) {
            throw invalid(handler, "is not a Todo 7 tenant control-plane route");
        }
    }

    /** 生成路由的"方法 + 路径"键集合，用于与公开路由白名单比对；未声明 HTTP 方法时报错。 */
    private static List<String> routeKeys(RequestMappingInfo mapping) {
        Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
            throw new IllegalStateException("Explicit public route classification requires an HTTP method");
        }
        return mapping.getPathPatternsCondition().getPatterns().stream()
                .flatMap(path -> methods.stream().map(method -> method.name() + " " + path.getPatternString()))
                .toList();
    }

    /** 构造带原因说明的配置错误异常（指明处理器方法与原因）。 */
    private static IllegalStateException invalid(HandlerMethod handler, String reason) {
        return new IllegalStateException("Management route " + handler.getBeanType().getName() + "#"
                + handler.getMethod().getName() + " " + reason);
    }

    /**
     * 路由条目：路径模式、允许的 HTTP 方法及其权限元数据。
     *
     * @param pattern  路径模式
     * @param methods  允许的 HTTP 方法集合（空表示不限方法）
     * @param metadata 该路由的权限元数据
     */
    private record Route(PathPattern pattern, Set<RequestMethod> methods, RouteMetadata metadata) {
        /** 判断给定路径与 HTTP 方法是否与该路由匹配。 */
        boolean matches(String path, HttpMethod method) {
            return (methods.isEmpty() || methods.stream().anyMatch(candidate -> candidate.name().equals(method.name())))
                    && pattern.matches(PathContainer.parsePath(path));
        }
    }

    /**
     * 路由权限元数据：租户权限、平台权限与路由分类三者恰好存一。
     *
     * @param tenant        租户级权限注解（有则要求租户上下文）
     * @param platform      平台级权限注解
     * @param classification 显式路由分类
     */
    private record RouteMetadata(
            TenantManagementPermission tenant,
            PlatformManagementPermission platform,
            ManagementRouteKind classification) {
        /** 是否需要先建立租户上下文（租户权限路由或授权上下文路由）。 */
        boolean requiresTenantContext() {
            return tenant != null || classification == ManagementRouteKind.AUTHORIZATION_CONTEXT;
        }
    }
}
