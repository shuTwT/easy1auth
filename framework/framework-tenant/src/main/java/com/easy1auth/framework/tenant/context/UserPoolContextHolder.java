package com.easy1auth.framework.tenant.context;

import java.util.Objects;
import java.util.UUID;

/**
 * 用户池上下文持有器：以线程局部变量（{@link ThreadLocal}）保存当前线程的用户池 ID
 * 与忽略用户池过滤标志，供过滤器、拦截器与领域服务在多用户池语义下读取。
 *
 * <p>上下文不会自动跨线程传播，向其他线程提交任务时需显式传播</p>
 */
public final class UserPoolContextHolder {
    /** 当前线程的用户池 ID（无用户池上下文时为 null） */
    private static final ThreadLocal<UUID> USER_POOL_ID = new ThreadLocal<>();
    /** 当前线程是否忽略用户池过滤（用于系统级数据访问） */
    private static final ThreadLocal<Boolean> IGNORE = new ThreadLocal<>();

    private UserPoolContextHolder() {
    }

    /** 返回当前线程的用户池 ID；未设置时返回 null。 */
    public static UUID getUserPoolId() {
        return USER_POOL_ID.get();
    }

    /** 返回当前线程的用户池 ID；缺失时抛出异常（用于必须处于用户池上下文的场景）。 */
    public static UUID requireUserPoolId() {
        return Objects.requireNonNull(USER_POOL_ID.get(), "UserPool context is unavailable");
    }

    /** 设置当前线程的用户池 ID；传入 null 时清除。 */
    public static void setUserPoolId(UUID userPoolId) {
        if (userPoolId == null) {
            USER_POOL_ID.remove();
        } else {
            USER_POOL_ID.set(userPoolId);
        }
    }

    /** 当前线程是否处于忽略用户池过滤模式。 */
    public static boolean isIgnore() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    /** 设置或清除忽略用户池过滤标志（true 忽略，false 清除）。 */
    public static void setIgnore(boolean ignore) {
        if (ignore) {
            IGNORE.set(Boolean.TRUE);
        } else {
            IGNORE.remove();
        }
    }

    /** 清除当前线程的用户池 ID 与忽略标志（如请求结束时的清理）。 */
    public static void clear() {
        USER_POOL_ID.remove();
        IGNORE.remove();
    }
}
