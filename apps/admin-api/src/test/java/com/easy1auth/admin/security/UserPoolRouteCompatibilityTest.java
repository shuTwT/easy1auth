package com.easy1auth.admin.security;

import com.easy1auth.admin.web.TenantController;
import com.easy1auth.admin.web.UserPoolController;
import com.easy1auth.admin.web.UserPoolPackageController;
import com.easy1auth.framework.web.util.WebFrameworkUtils;
import com.easy1auth.system.constant.ManagementPermissionCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class UserPoolRouteCompatibilityTest {
    @Test
    void poolAndBusinessTenantRoutesHaveIndependentManagementContexts() {
        var mvc = MockMvcBuilders.standaloneSetup(mock(UserPoolController.class), mock(UserPoolPackageController.class), mock(TenantController.class)).build();
        var mappings = mvc.getDispatcherServlet().getWebApplicationContext().getBean(RequestMappingHandlerMapping.class);
        var inventory = new ManagementRouteInventory(mappings);
        inventory.afterSingletonsInstantiated();
        assertFalse(inventory.requiresUserPoolContext(request("GET", "/api/user-pools/list")));
        assertFalse(inventory.requiresUserPoolContext(request("GET", "/api/tenants/list")));
        assertFalse(inventory.requiresUserPoolContext(request("GET", "/api/platform/user-pool-packages")));
        assertFalse(inventory.requiresUserPoolContext(request("GET", "/api/platform/tenant-packages")));
        assertTrue(inventory.requiresUserPoolContext(request("GET", "/api/pool-tenants")));
        assertEquals(ManagementPermissionCode.POOL_TENANT_LIST,
                inventory.tenantPermission(request("GET", "/api/pool-tenants")).orElseThrow());
        assertEquals(ManagementPermissionCode.POOL_TENANT_CREATE,
                inventory.tenantPermission(request("POST", "/api/pool-tenants")).orElseThrow());
        var paths = mappings.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .toList();
        assertTrue(paths.contains("/api/user-pools/list"));
        assertTrue(paths.contains("/api/tenants/list"));
        assertTrue(paths.contains("/api/platform/user-pool-packages"));
        assertTrue(paths.contains("/api/platform/tenant-packages"));
    }

    @Test
    void newHeaderAndLegacyAliasBothIdentifyUserPools() {
        UUID userPoolId = UUID.randomUUID();
        var legacy = request("GET", "/api/pool-tenants");
        legacy.addHeader("tenant-id", userPoolId);
        assertEquals(userPoolId, WebFrameworkUtils.getUserPoolId(legacy));
        var current = request("GET", "/api/pool-tenants");
        current.addHeader("user-pool-id", userPoolId);
        assertEquals(userPoolId, WebFrameworkUtils.getUserPoolId(current));
        current.addHeader("tenant-id", UUID.randomUUID());
        assertEquals(userPoolId, WebFrameworkUtils.getUserPoolId(current));
        assertNull(WebFrameworkUtils.getUserPoolId(request("GET", "/api/pool-tenants")));
    }

    private static MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
