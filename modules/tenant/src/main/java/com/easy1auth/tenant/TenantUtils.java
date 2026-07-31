package com.easy1auth.tenant;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

public final class TenantUtils {
    private TenantUtils() {
    }

    public static void execute(UUID tenantId, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        Scope previous = enterTenant(tenantId);
        try {
            runnable.run();
        } finally {
            previous.restore();
        }
    }

    public static <V> V execute(UUID tenantId, Callable<V> callable) throws Exception {
        Objects.requireNonNull(callable, "callable");
        Scope previous = enterTenant(tenantId);
        try {
            return callable.call();
        } finally {
            previous.restore();
        }
    }

    public static void executeIgnore(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        Scope previous = enterIgnore();
        try {
            runnable.run();
        } finally {
            previous.restore();
        }
    }

    public static <V> V executeIgnore(Callable<V> callable) throws Exception {
        Objects.requireNonNull(callable, "callable");
        Scope previous = enterIgnore();
        try {
            return callable.call();
        } finally {
            previous.restore();
        }
    }

    private static Scope enterTenant(UUID tenantId) {
        Scope previous = Scope.capture();
        TenantContextHolder.setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        TenantContextHolder.setIgnore(false);
        return previous;
    }

    private static Scope enterIgnore() {
        Scope previous = Scope.capture();
        TenantContextHolder.setIgnore(true);
        return previous;
    }

    private record Scope(UUID tenantId, boolean ignore) {
        private static Scope capture() {
            return new Scope(TenantContextHolder.getTenantId(), TenantContextHolder.isIgnore());
        }

        private void restore() {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setIgnore(ignore);
        }
    }
}
