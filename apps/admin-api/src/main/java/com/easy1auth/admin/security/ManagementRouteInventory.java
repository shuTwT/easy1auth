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

@Component
public final class ManagementRouteInventory implements SmartInitializingSingleton {
    private static final String CONTROLLER_PACKAGE = "com.easy1auth.admin.web";
    private static final Set<String> PUBLIC_ROUTES = Set.of("GET /api/login-style/public");

    private final RequestMappingHandlerMapping mappings;
    private volatile List<Route> routes = List.of();

    ManagementRouteInventory(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings) {
        this.mappings = mappings;
    }

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

    public boolean requiresTenantContext(HttpServletRequest request) {
        return metadata(request).map(RouteMetadata::requiresTenantContext).orElse(false);
    }

    public Optional<ManagementPermissionCode> tenantPermission(HttpServletRequest request) {
        return metadata(request)
                .map(RouteMetadata::tenant)
                .map(TenantManagementPermission::value);
    }

    private Optional<RouteMetadata> metadata(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        return routes.stream()
                .filter(route -> route.matches(path, method))
                .map(Route::metadata)
                .findFirst();
    }

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

    private static void validateTenant(HandlerMethod handler, TenantManagementPermission requirement) {
        ManagementPermissionCode code = requirement.value();
        if (code.scope() != ManagementPermissionScope.TENANT
                || (code.type() != ManagementPermissionType.ACTION && code.type() != ManagementPermissionType.MENU)) {
            throw invalid(handler, "has an invalid tenant permission");
        }
    }

    private static void validatePlatform(HandlerMethod handler, PlatformManagementPermission requirement) {
        ManagementPermissionCode code = requirement.value();
        if (code.scope() != ManagementPermissionScope.PLATFORM || code.type() != ManagementPermissionType.ACTION) {
            throw invalid(handler, "has an invalid platform permission");
        }
    }

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

    private static List<String> routeKeys(RequestMappingInfo mapping) {
        Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
            throw new IllegalStateException("Explicit public route classification requires an HTTP method");
        }
        return mapping.getPathPatternsCondition().getPatterns().stream()
                .flatMap(path -> methods.stream().map(method -> method.name() + " " + path.getPatternString()))
                .toList();
    }

    private static IllegalStateException invalid(HandlerMethod handler, String reason) {
        return new IllegalStateException("Management route " + handler.getBeanType().getName() + "#"
                + handler.getMethod().getName() + " " + reason);
    }

    private record Route(PathPattern pattern, Set<RequestMethod> methods, RouteMetadata metadata) {
        boolean matches(String path, HttpMethod method) {
            return (methods.isEmpty() || methods.stream().anyMatch(candidate -> candidate.name().equals(method.name())))
                    && pattern.matches(PathContainer.parsePath(path));
        }
    }

    private record RouteMetadata(
            TenantManagementPermission tenant,
            PlatformManagementPermission platform,
            ManagementRouteKind classification) {
        boolean requiresTenantContext() {
            return tenant != null || classification == ManagementRouteKind.AUTHORIZATION_CONTEXT;
        }
    }
}
