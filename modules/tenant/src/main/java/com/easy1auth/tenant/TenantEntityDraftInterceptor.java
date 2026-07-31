package com.easy1auth.tenant;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.persistence.model.BaseTenantEntity;
import com.easy1auth.persistence.model.BaseTenantEntityDraft;
import com.easy1auth.persistence.model.BaseTenantEntityProps;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public final class TenantEntityDraftInterceptor
        implements DraftInterceptor<BaseTenantEntity, BaseTenantEntityDraft> {

    @Override
    public void beforeSave(BaseTenantEntityDraft draft, @Nullable BaseTenantEntity original) {
        if (TenantContextHolder.isIgnore()) {
            return;
        }
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return;
        }
        if (original != null && !tenantId.equals(original.tenantId())) {
            throw mismatch();
        }
        if (ImmutableObjects.isLoaded(draft, BaseTenantEntityProps.TENANT_ID)) {
            if (!tenantId.equals(draft.tenantId())) {
                throw mismatch();
            }
        } else {
            draft.setTenantId(tenantId);
        }
    }

    @Override
    public Collection<TypedProp<BaseTenantEntity, ?>> dependencies() {
        return List.of(BaseTenantEntityProps.TENANT_ID);
    }

    private static DomainException mismatch() {
        return new DomainException("TENANT_CONTEXT_MISMATCH", "数据租户与当前租户不一致", 403);
    }
}
