package com.easy1auth.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class WebFrameworkTest {
    @Test
    void exposesTenantSpecificRequestOperations() {
        UUID tenantId = UUID.randomUUID();
        var request = new MockHttpServletRequest();
        request.addHeader(WebFramework.TENANT_ID_HEADER, tenantId.toString());
        request.addHeader(WebFramework.USER_AGENT_HEADER, "test-agent");
        var context = new TenantContext(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), "tenant_admin",
                Set.of("user:list"), null, "trace-1");

        WebFramework.setTenantContext(request, context);

        assertEquals(tenantId, WebFramework.getTenantId(request));
        assertEquals("test-agent", WebFramework.getUserAgent(request));
        assertSame(context, WebFramework.getTenantContext(request));
        assertSame(context, WebFramework.requireTenantContext(request));
    }

    @Test
    void returnsNullWhenTenantValuesAreMissing() {
        var request = new MockHttpServletRequest();
        assertNull(WebFramework.getTenantId(request));
        assertNull(WebFramework.getTenantContext(request));
    }
}
