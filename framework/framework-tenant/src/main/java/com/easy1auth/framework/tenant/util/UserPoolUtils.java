package com.easy1auth.framework.tenant.util;

import com.easy1auth.framework.tenant.context.UserPoolContextHolder;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * 用户池作用域执行工具。
 *
 * <p>提供在指定用户池上下文（或忽略用户池过滤模式）下执行代码的能力：
 * 进入作用域前保存当前线程的用户池上下文，执行结束后自动恢复。
 * 由于用户池上下文基于 {@link ThreadLocal} 且不会自动跨线程传播，
 * 本工具可用于异步/其他线程任务的显式传播。</p>
 */
public final class UserPoolUtils {
    private UserPoolUtils() {
    }

    /** 在指定用户池上下文中执行任务，结束后恢复原用户池上下文。 */
    public static void execute(UUID userPoolId, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        Scope previous = enterUserPool(userPoolId);
        try {
            runnable.run();
        } finally {
            previous.restore();
        }
    }

    /** 在指定用户池上下文中执行可返回值的任务，结束后恢复原用户池上下文。 */
    public static <V> V execute(UUID userPoolId, Callable<V> callable) throws Exception {
        Objects.requireNonNull(callable, "callable");
        Scope previous = enterUserPool(userPoolId);
        try {
            return callable.call();
        } finally {
            previous.restore();
        }
    }

    /** 在忽略用户池过滤的模式下执行任务，结束后恢复原用户池上下文（用于系统级数据访问）。 */
    public static void executeIgnore(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        Scope previous = enterIgnore();
        try {
            runnable.run();
        } finally {
            previous.restore();
        }
    }

    /** 在忽略用户池过滤的模式下执行可返回值的任务，结束后恢复原用户池上下文。 */
    public static <V> V executeIgnore(Callable<V> callable) throws Exception {
        Objects.requireNonNull(callable, "callable");
        Scope previous = enterIgnore();
        try {
            return callable.call();
        } finally {
            previous.restore();
        }
    }

    /** 进入指定用户池作用域并返回调用前的上下文快照（内部工具方法）。 */
    private static Scope enterUserPool(UUID userPoolId) {
        Scope previous = Scope.capture();
        UserPoolContextHolder.setUserPoolId(Objects.requireNonNull(userPoolId, "userPoolId"));
        UserPoolContextHolder.setIgnore(false);
        return previous;
    }

    /** 进入忽略用户池过滤的作用域并返回调用前的上下文快照（内部工具方法）。 */
    private static Scope enterIgnore() {
        Scope previous = Scope.capture();
        UserPoolContextHolder.setIgnore(true);
        return previous;
    }

    /**
     * 用户池作用域快照，用于在执行结束后恢复调用前的线程用户池上下文。
     *
     * @param userPoolId 快照时的用户池 ID（可为 null）
     * @param ignore   快照时是否处于忽略用户池过滤模式
     */
    private record Scope(UUID userPoolId, boolean ignore) {
        /** 捕获当前线程的用户池上下文快照。 */
        private static Scope capture() {
            return new Scope(UserPoolContextHolder.getUserPoolId(), UserPoolContextHolder.isIgnore());
        }

        /** 将线程用户池上下文恢复为快照时的状态。 */
        private void restore() {
            UserPoolContextHolder.setUserPoolId(userPoolId);
            UserPoolContextHolder.setIgnore(ignore);
        }
    }
}
