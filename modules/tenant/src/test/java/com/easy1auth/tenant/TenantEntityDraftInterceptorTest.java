package com.easy1auth.tenant;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.tenant.model.TenantMembershipEntityDraft;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantEntityDraftInterceptorTest {
    private final TenantEntityDraftInterceptor interceptor = new TenantEntityDraftInterceptor();

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void fillsMissingTenantFromCurrentContext() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        var entity = TenantMembershipEntityDraft.$.produce(draft -> interceptor.beforeSave(draft, null));

        assertEquals(tenantId, entity.tenantId());
    }

    @Test
    void rejectsTenantDifferentFromCurrentContext() {
        TenantContextHolder.setTenantId(UUID.randomUUID());

        DomainException exception = assertThrows(DomainException.class,
                () -> TenantMembershipEntityDraft.$.produce(draft -> {
                    draft.setTenantId(UUID.randomUUID());
                    interceptor.beforeSave(draft, null);
                }));

        assertEquals(ErrorCodeConstants.TENANT_CONTEXT_MISMATCH.code(), exception.code());
    }
}
