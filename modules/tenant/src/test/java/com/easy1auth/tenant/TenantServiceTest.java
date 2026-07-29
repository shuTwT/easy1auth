package com.easy1auth.tenant;

import com.easy1auth.foundation.error.DomainException;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantServiceTest {
    private final TenantRepository repository = mock(TenantRepository.class);
    private final TenantAuthorizationProvider authorization = mock(TenantAuthorizationProvider.class);
    private final TenantService service = new TenantService(repository, authorization);

    @Test void rejectsForgedTenantSelection() {
        var account = UUID.randomUUID(); var tenant = UUID.randomUUID();
        assertThatThrownBy(() -> service.resolve(account, tenant, "trace"))
                .isInstanceOf(DomainException.class).extracting("code").isEqualTo("TENANT_ACCESS_DENIED");
    }

    @Test void ownerCannotRemoveSelf() {
        var owner = UUID.randomUUID(); var tenant = UUID.randomUUID();
        when(repository.role(owner, tenant)).thenReturn("owner");
        assertThatThrownBy(() -> service.removeMember(owner, tenant, owner))
                .isInstanceOf(DomainException.class).extracting("code").isEqualTo("OWNER_CANNOT_REMOVE_SELF");
        verify(repository, never()).remove(any(), any());
    }

    @Test void transferIsSerializedAndRequiresTargetMembership() {
        var owner = UUID.randomUUID(); var target = UUID.randomUUID(); var tenant = UUID.randomUUID();
        when(repository.role(owner, tenant)).thenReturn("owner");
        when(repository.role(target, tenant)).thenReturn("member");
        service.transferOwner(owner, tenant, target);
        var ordered = inOrder(repository);
        ordered.verify(repository).lockTenant(tenant);
        ordered.verify(repository).role(owner, tenant);
        ordered.verify(repository).role(target, tenant);
        ordered.verify(repository).transfer(tenant, owner, target);
    }
}
