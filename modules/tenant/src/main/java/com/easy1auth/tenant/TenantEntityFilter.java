package com.easy1auth.tenant;

import com.easy1auth.persistence.model.BaseTenantEntityProps;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 租户实体全局过滤器。
 *
 * <p>实现 jimmer 的 {@link Filter}：对所有继承 {@link BaseTenantEntity} 的实体查询
 * 自动追加“tenantId = 当前租户”的过滤条件，从读取侧实现数据层面的租户隔离；
 * 忽略标志开启或当前无租户上下文时不过滤。</p>
 */
@Component
public final class TenantEntityFilter implements Filter<BaseTenantEntityProps> {
    /** 为查询追加当前租户的过滤条件（除非处于忽略模式或无租户上下文）。 */
    @Override
    public void filter(FilterArgs<BaseTenantEntityProps> args) {
        if (TenantContextHolder.isIgnore()) {
            return;
        }
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            args.where(args.getTable().tenantId().eq(tenantId));
        }
    }
}
