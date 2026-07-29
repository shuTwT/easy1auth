package com.easy1auth.adminaccess;
import com.easy1auth.foundation.error.DomainException; import com.easy1auth.tenant.TenantContext; import org.babyfish.jimmer.sql.JSqlClient; import org.junit.jupiter.api.Test; import java.util.*; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
class AdminAccessServiceTest{
 @Test void cannotGrantPermissionsActorDoesNotOwn(){var service=new AdminAccessService(mock(JSqlClient.class));var context=new TenantContext(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"admin",Set.of("manager"),Set.of("admin-role:create"),"trace");assertThatThrownBy(()->service.create(context,"role",null,List.of("admin-user:status"))).isInstanceOf(DomainException.class).extracting("code").isEqualTo("PERMISSION_ESCALATION");}
 @Test void ownerWildcardSatisfiesEveryPermission(){var service=new AdminAccessService(mock(JSqlClient.class));var context=new TenantContext(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"owner",Set.of("owner"),Set.of("*"),"trace");assertThatCode(()->service.require(context,"admin-role:delete")).doesNotThrowAnyException();}
}
