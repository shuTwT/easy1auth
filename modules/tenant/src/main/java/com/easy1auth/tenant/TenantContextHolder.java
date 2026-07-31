package com.easy1auth.tenant;

import java.util.UUID;

/**
 * Holds the tenant scope associated with the current thread.
 *
 * <p>The holder deliberately uses a regular {@link ThreadLocal}. Tenant context must be
 * propagated explicitly when work is submitted to another thread.</p>
 */
public final class TenantContextHolder {
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static UUID getTenantId() {
        return TENANT_ID.get();
    }

    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            TENANT_ID.remove();
        } else {
            TENANT_ID.set(tenantId);
        }
    }

    public static boolean isIgnore() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    public static void setIgnore(boolean ignore) {
        if (ignore) {
            IGNORE.set(Boolean.TRUE);
        } else {
            IGNORE.remove();
        }
    }

    public static void clear() {
        TENANT_ID.remove();
        IGNORE.remove();
    }
}
