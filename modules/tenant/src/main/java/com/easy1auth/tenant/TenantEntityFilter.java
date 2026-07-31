package com.easy1auth.tenant;

import com.easy1auth.persistence.model.BaseTenantEntityProps;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class TenantEntityFilter implements Filter<BaseTenantEntityProps> {
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
