package com.easy1auth.tenant.util;

import java.util.Objects;
import java.util.UUID;

/**
 * 租户上下文持有器：以线程局部变量（{@link ThreadLocal}）保存当前线程的租户 ID
 * 与忽略租户过滤标志，供过滤器、拦截器与领域服务在多租户语义下读取。
 *
 * <p>上下文不会自动跨线程传播，向其他线程提交任务时需显式传播
 * （参见 {@link TenantUtils}）。</p>
 */
public final class TenantContextHolder {
    /** 当前线程的租户 ID（无租户上下文时为 null） */
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    /** 当前线程是否忽略租户过滤（用于系统级数据访问） */
    private static final ThreadLocal<Boolean> IGNORE = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    /** 返回当前线程的租户 ID；未设置时返回 null。 */
    public static UUID getTenantId() {
        return TENANT_ID.get();
    }

    /** 返回当前线程的租户 ID；缺失时抛出异常（用于必须处于租户上下文的场景）。 */
    public static UUID requireTenantId() {
        return Objects.requireNonNull(TENANT_ID.get(), "Tenant context is unavailable");
    }

    /** 设置当前线程的租户 ID；传入 null 时清除。 */
    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            TENANT_ID.remove();
        } else {
            TENANT_ID.set(tenantId);
        }
    }

    /** 当前线程是否处于忽略租户过滤模式。 */
    public static boolean isIgnore() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    /** 设置或清除忽略租户过滤标志（true 忽略，false 清除）。 */
    public static void setIgnore(boolean ignore) {
        if (ignore) {
            IGNORE.set(Boolean.TRUE);
        } else {
            IGNORE.remove();
        }
    }

    /** 清除当前线程的租户 ID 与忽略标志（如请求结束时的清理）。 */
    public static void clear() {
        TENANT_ID.remove();
        IGNORE.remove();
    }
}
