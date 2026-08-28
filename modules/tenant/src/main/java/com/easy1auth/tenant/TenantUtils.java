package com.easy1auth.tenant;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * 租户作用域执行工具。
 *
 * <p>提供在指定租户上下文（或忽略租户过滤模式）下执行代码的能力：
 * 进入作用域前保存当前线程的租户上下文，执行结束后自动恢复。
 * 由于租户上下文基于 {@link ThreadLocal} 且不会自动跨线程传播，
 * 本工具可用于异步/其他线程任务的显式传播。</p>
 */
public final class TenantUtils {
    private TenantUtils() {
    }

    /** 在指定租户上下文中执行任务，结束后恢复原租户上下文。 */
    public static void execute(UUID tenantId, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        Scope previous = enterTenant(tenantId);
        try {
            runnable.run();
        } finally {
            previous.restore();
        }
    }

    /** 在指定租户上下文中执行可返回值的任务，结束后恢复原租户上下文。 */
    public static <V> V execute(UUID tenantId, Callable<V> callable) throws Exception {
        Objects.requireNonNull(callable, "callable");
        Scope previous = enterTenant(tenantId);
        try {
            return callable.call();
        } finally {
            previous.restore();
        }
    }

    /** 在忽略租户过滤的模式下执行任务，结束后恢复原租户上下文（用于系统级数据访问）。 */
    public static void executeIgnore(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        Scope previous = enterIgnore();
        try {
            runnable.run();
        } finally {
            previous.restore();
        }
    }

    /** 在忽略租户过滤的模式下执行可返回值的任务，结束后恢复原租户上下文。 */
    public static <V> V executeIgnore(Callable<V> callable) throws Exception {
        Objects.requireNonNull(callable, "callable");
        Scope previous = enterIgnore();
        try {
            return callable.call();
        } finally {
            previous.restore();
        }
    }

    /** 进入指定租户作用域并返回调用前的上下文快照（内部工具方法）。 */
    private static Scope enterTenant(UUID tenantId) {
        Scope previous = Scope.capture();
        TenantContextHolder.setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        TenantContextHolder.setIgnore(false);
        return previous;
    }

    /** 进入忽略租户过滤的作用域并返回调用前的上下文快照（内部工具方法）。 */
    private static Scope enterIgnore() {
        Scope previous = Scope.capture();
        TenantContextHolder.setIgnore(true);
        return previous;
    }

    /**
     * 租户作用域快照，用于在执行结束后恢复调用前的线程租户上下文。
     *
     * @param tenantId 快照时的租户 ID（可为 null）
     * @param ignore   快照时是否处于忽略租户过滤模式
     */
    private record Scope(UUID tenantId, boolean ignore) {
        /** 捕获当前线程的租户上下文快照。 */
        private static Scope capture() {
            return new Scope(TenantContextHolder.getTenantId(), TenantContextHolder.isIgnore());
        }

        /** 将线程租户上下文恢复为快照时的状态。 */
        private void restore() {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setIgnore(ignore);
        }
    }
}
