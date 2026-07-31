package com.easy1auth.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantUtilsTest {
    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void executesWithSpecifiedTenantAndRestoresPreviousScope() {
        UUID outerTenant = UUID.randomUUID();
        UUID innerTenant = UUID.randomUUID();
        TenantContextHolder.setTenantId(outerTenant);
        TenantContextHolder.setIgnore(true);

        TenantUtils.execute(innerTenant, () -> {
            assertEquals(innerTenant, TenantContextHolder.getTenantId());
            assertFalse(TenantContextHolder.isIgnore());
        });

        assertEquals(outerTenant, TenantContextHolder.getTenantId());
        assertTrue(TenantContextHolder.isIgnore());
    }

    @Test
    void supportsNestedTenantAndIgnoreScopes() {
        UUID outerTenant = UUID.randomUUID();
        UUID innerTenant = UUID.randomUUID();

        TenantUtils.execute(outerTenant, () -> {
            TenantUtils.executeIgnore(() -> {
                assertEquals(outerTenant, TenantContextHolder.getTenantId());
                assertTrue(TenantContextHolder.isIgnore());

                TenantUtils.execute(innerTenant, () -> {
                    assertEquals(innerTenant, TenantContextHolder.getTenantId());
                    assertFalse(TenantContextHolder.isIgnore());
                });

                assertEquals(outerTenant, TenantContextHolder.getTenantId());
                assertTrue(TenantContextHolder.isIgnore());
            });

            assertEquals(outerTenant, TenantContextHolder.getTenantId());
            assertFalse(TenantContextHolder.isIgnore());
        });

        assertNull(TenantContextHolder.getTenantId());
        assertFalse(TenantContextHolder.isIgnore());
    }

    @Test
    void restoresScopeWhenRunnableFails() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        assertThrows(IllegalStateException.class,
                () -> TenantUtils.executeIgnore((Runnable) () -> {
                    throw new IllegalStateException("failed");
                }));

        assertEquals(tenantId, TenantContextHolder.getTenantId());
        assertFalse(TenantContextHolder.isIgnore());
    }

    @Test
    void callableReturnsValueAndRestoresScopeWhenItFails() throws Exception {
        UUID tenantId = UUID.randomUUID();
        assertEquals("result", TenantUtils.execute(tenantId, () -> {
            assertEquals(tenantId, TenantContextHolder.getTenantId());
            return "result";
        }));

        Exception exception = assertThrows(Exception.class,
                () -> TenantUtils.execute(tenantId, () -> {
                    throw new Exception("failed");
                }));

        assertEquals("failed", exception.getMessage());
        assertNull(TenantContextHolder.getTenantId());
        assertFalse(TenantContextHolder.isIgnore());
    }
}
