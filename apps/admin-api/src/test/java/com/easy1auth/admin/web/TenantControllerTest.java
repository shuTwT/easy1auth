package com.easy1auth.admin.web;

import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.framework.tenant.context.UserPoolContextHolder;
import com.easy1auth.tenant.dto.TenantInput;
import com.easy1auth.tenant.dto.UserPoolAccess;
import com.easy1auth.tenant.service.TenantService;
import com.easy1auth.tenant.service.UserPoolService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TenantControllerTest {
    private final TenantService tenants = mock(TenantService.class);
    private final UserPoolService pools = mock(UserPoolService.class);
    private final TenantController controller = new TenantController(tenants, pools);
    private final UUID accountId = UUID.randomUUID();
    private final UUID poolId = UUID.randomUUID();
    private final Principal principal = () -> accountId.toString();

    @AfterEach
    void cleanup() {
        UserPoolContextHolder.clear();
    }

    @Test
    void missingPoolContextDoesNotReachTenantService() {
        assertThrows(DomainException.class, () -> controller.list(principal, 1, 20, null, null));
        verifyNoInteractions(tenants, pools);
    }

    @Test
    void poolPermissionIsRequiredBeforeTenantMutation() {
        UserPoolContextHolder.setUserPoolId(poolId);
        when(pools.authorize(accountId, poolId)).thenReturn(new UserPoolAccess(poolId, "common", Set.of("pool-tenant:list"), null));
        assertThrows(DomainException.class, () -> controller.create(principal, new TenantInput("企业", "company", null, "active")));
        verifyNoInteractions(tenants);
    }

    @Test
    void forgedPoolHeaderIsRejectedByMembershipAuthorization() {
        UserPoolContextHolder.setUserPoolId(poolId);
        when(pools.authorize(accountId, poolId)).thenThrow(new DomainException(com.easy1auth.tenant.constant.ErrorCodeConstants.USER_POOL_ACCESS_DENIED));
        assertThrows(DomainException.class, () -> controller.list(principal, 1, 20, null, null));
        verifyNoInteractions(tenants);
    }

    @Test
    void authorizedMutationUsesPoolFromContext() {
        UserPoolContextHolder.setUserPoolId(poolId);
        when(pools.authorize(accountId, poolId)).thenReturn(new UserPoolAccess(poolId, "tenant_admin", Set.of("pool-tenant:create"), null));
        var input = new TenantInput("企业", "company", null, "active");
        controller.create(principal, input);
        verify(tenants).create(poolId, input);
    }
}
