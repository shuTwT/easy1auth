package com.easy1auth.admin;

import com.easy1auth.directory.PoolUserService;
import com.easy1auth.directory.DirectoryCatalogService;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.tenant.TenantService;
import com.easy1auth.useraccess.UserAccessCatalogService;
import com.easy1auth.application.ApplicationService;
import com.easy1auth.security.*; import com.easy1auth.federation.FederationService; import com.easy1auth.customization.CustomizationService; import com.easy1auth.audit.AuditService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

@SpringBootTest(properties = {"spring.flyway.enabled=true", "easy1auth.admin-jwt.secret=01234567890123456789012345678901", "easy1auth.security.data-encryption-secret=abcdefghijklmnopqrstuvwxyz012345"})
@Testcontainers(disabledWithoutDocker = true)
class Phase4PostgresIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired JdbcClient db; @Autowired TenantService tenants; @Autowired PoolUserService users;
    @Autowired DirectoryCatalogService directory; @Autowired UserAccessCatalogService access;
    @Autowired ApplicationService applications;
    @Autowired SecurityPolicyService security; @Autowired TotpService totp; @Autowired FederationService federation;
    @Autowired CustomizationService customization; @Autowired AuditService audit;

    @Test void knownUserIdCannotCrossTenantBoundary() {
        UUID a=account("a"), b=account("b"); var tenantA=tenants.create(a,"A","basic"); var tenantB=tenants.create(b,"B","basic");
        var user=users.create(tenantA.id(),input("alice","alice@example.com"));
        assertThatThrownBy(()->users.get(tenantB.id(),user.id())).isInstanceOf(DomainException.class).extracting("code").isEqualTo("POOL_USER_NOT_FOUND");
        assertThatThrownBy(()->users.delete(tenantB.id(),user.id())).isInstanceOf(DomainException.class).extracting("code").isEqualTo("POOL_USER_NOT_FOUND");
    }

    @Test void concurrentCreationCannotBreakTenantQuota() throws Exception {
        UUID account=account("quota"); var tenant=tenants.create(account,"Quota","basic");
        db.sql("update tenant set max_users=1 where id=:id").param("id",tenant.id()).update();
        try(var executor=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            List<Future<Boolean>> results=List.of(executor.submit(()->createAfter(start,tenant.id(),"u1","u1@example.com")),executor.submit(()->createAfter(start,tenant.id(),"u2","u2@example.com")));
            start.countDown(); long successes=0; for(var result:results)if(result.get(20,TimeUnit.SECONDS))successes++;
            assertThat(successes).isEqualTo(1); assertThat(users.stats(tenant.id()).get("totalUsers")).isEqualTo(1L);
        }
    }

    @Test void hierarchyCyclesAreRejectedForGroupsRolesAndPermissions() {
        UUID account=account("cycles"); var tenant=tenants.create(account,"Cycles","basic");

        var groupRoot=directory.createGroup(tenant.id(),new DirectoryCatalogService.GroupInput("root",null,"department",null));
        var groupChild=directory.createGroup(tenant.id(),new DirectoryCatalogService.GroupInput("child",null,"team",groupRoot.id()));
        assertCode("GROUP_CYCLE",()->directory.updateGroup(tenant.id(),groupRoot.id(),new DirectoryCatalogService.GroupInput(null,null,null,groupChild.id())));

        var roleRoot=access.createRole(tenant.id(),role("root-role","root-role","self",null,Map.of()));
        var roleChild=access.createRole(tenant.id(),role("child-role","child-role","department",roleRoot.id(),Map.of()));
        assertCode("ROLE_CYCLE",()->access.updateRole(tenant.id(),roleRoot.id(),role(null,null,null,roleChild.id(),null)));

        var permissionRoot=access.createPermission(tenant.id(),permission("root:read","root",null));
        var permissionChild=access.createPermission(tenant.id(),permission("child:read","child",permissionRoot.id()));
        assertCode("PERMISSION_CYCLE",()->access.updatePermission(tenant.id(),permissionRoot.id(),new UserAccessCatalogService.PermissionInput(null,null,null,null,permissionChild.id(),null,null)));
    }

    @Test void crossTenantGroupAndRoleAssignmentsAreRejected() {
        UUID a=account("assign-a"),b=account("assign-b"); var tenantA=tenants.create(a,"Assign A","basic"); var tenantB=tenants.create(b,"Assign B","basic");
        var userA=users.create(tenantA.id(),input("assign-a","assign-a@example.com"));
        var groupB=directory.createGroup(tenantB.id(),new DirectoryCatalogService.GroupInput("B group",null,"team",null));
        var roleB=access.createRole(tenantB.id(),role("B role","b-role","self",null,Map.of()));

        assertCode("DIRECTORY_ITEM_NOT_FOUND",()->directory.addMembers(tenantB.id(),groupB.id(),List.of(userA.id())));
        assertCode("USER_ACCESS_NOT_FOUND",()->access.assignUsers(tenantB.id(),roleB.id(),List.of(userA.id())));
        assertCode("USER_ACCESS_NOT_FOUND",()->access.replaceUserRoles(tenantA.id(),userA.id(),List.of(roleB.id())));
    }

    @Test void systemRolesAreReservedAndCannotBeMutatedOrDeleted() {
        UUID account=account("system-role"); var tenant=tenants.create(account,"System role","basic");
        assertCode("SYSTEM_ROLE_RESERVED",()->access.createRole(tenant.id(),new UserAccessCatalogService.RoleInput("Fake system","fake-system",null,"system",Map.of(),"all",null)));
        var role=access.createRole(tenant.id(),role("Managed","managed","all",null,Map.of("user:read",true)));
        db.sql("update pool_role set type='system' where id=:id and tenant_id=:tenant").param("id",role.id()).param("tenant",tenant.id()).update();
        assertCode("SYSTEM_ROLE_IMMUTABLE",()->access.updateRole(tenant.id(),role.id(),role("Changed",null,"self",null,null)));
        assertCode("SYSTEM_ROLE_IMMUTABLE",()->access.deleteRole(tenant.id(),role.id()));
    }

    @Test void dataScopeUsesMostPermissiveAssignedRoleAndJsonPermissionsRoundTrip() {
        UUID account=account("scope"); var tenant=tenants.create(account,"Scope","basic");
        var user=users.create(tenant.id(),input("scope-user","scope@example.com"));
        var self=access.createRole(tenant.id(),role("Self","scope-self","self",null,Map.of("user:read",true)));
        var department=access.createRole(tenant.id(),role("Department","scope-department","department",null,Map.of()));
        var subtree=access.createRole(tenant.id(),role("Subtree","scope-subtree","department_and_sub",null,Map.of()));
        var all=access.createRole(tenant.id(),role("All","scope-all","all",null,Map.of()));

        assertThat(access.role(tenant.id(),self.id()).permissions()).containsEntry("user:read",true);
        access.replaceUserRoles(tenant.id(),user.id(),List.of(self.id(),department.id()));
        assertThat(access.effectiveDataScope(tenant.id(),user.id())).isEqualTo("department");
        access.replaceUserRoles(tenant.id(),user.id(),List.of(self.id(),department.id(),subtree.id()));
        assertThat(access.effectiveDataScope(tenant.id(),user.id())).isEqualTo("department_and_sub");
        access.replaceUserRoles(tenant.id(),user.id(),List.of(self.id(),department.id(),subtree.id(),all.id()));
        assertThat(access.effectiveDataScope(tenant.id(),user.id())).isEqualTo("all");
    }

    @Test void permissionPresetsAreIdempotent() {
        UUID account=account("presets"); var tenant=tenants.create(account,"Presets","basic");
        long first=access.permissionStats(tenant.id()).get("totalPermissions");
        long second=access.permissionStats(tenant.id()).get("totalPermissions");
        assertThat(first).isPositive();
        assertThat(second).isEqualTo(first);
    }

    @Test void applicationSecretIsReturnedOnceAndKnownIdCannotCrossTenantBoundary(){
        UUID a=account("app-a"),b=account("app-b");var tenantA=tenants.create(a,"App A","basic");var tenantB=tenants.create(b,"App B","basic");
        var created=applications.create(tenantA.id(),new ApplicationService.ApplicationInput("Web",null,null,"web",List.of("https://client.example/callback"),List.of(),List.of("authorization_code","refresh_token"),List.of("openid","profile"),true,true,900,3600));
        assertThat(created.clientSecret()).isNotBlank();assertThat(applications.get(tenantA.id(),created.id()).clientSecret()).isNull();
        String hash=db.sql("select client_secret_hash from oauth_application where id=:id").param("id",created.id()).query(String.class).single();assertThat(hash).isNotEqualTo(created.clientSecret()).startsWith("$2");
        assertCode("APPLICATION_NOT_FOUND",()->applications.get(tenantB.id(),created.id()));
    }

    @Test void phase6SecurityFederationCustomizationAndAuditAreTenantIsolated(){
        UUID a=account("phase6-a"),b=account("phase6-b");var tenantA=tenants.create(a,"Phase6 A","basic");var tenantB=tenants.create(b,"Phase6 B","basic");
        var policy=security.update(tenantA.id(),new SecurityPolicyService.Policy(14,true,true,true,true,60,6,true,4,900));
        assertThat(policy.minLength()).isEqualTo(14);assertThat(security.policy(tenantB.id()).minLength()).isEqualTo(8);
        UUID subject=UuidV7.randomUuid();var setup=security.setupTotp("pool_user",subject,tenantA.id(),"phase6@example.com");security.enableTotp("pool_user",subject,totp.code(setup.secret(),totp.step(java.time.Instant.now())));
        assertThat(security.status("pool_user",subject).enabled()).isTrue();
        var provider=federation.create(tenantA.id(),new FederationService.Input("Corporate OIDC","https://id.example.com","client","top-secret",List.of("openid","email"),Map.of("email","email"),false,null));
        assertThat(provider.clientSecret()).isEqualTo("top-secret");assertThat(db.sql("select encrypted_client_secret from federation_provider where id=:id").param("id",provider.id()).query(String.class).single()).doesNotContain("top-secret");
        assertCode("OIDC_PROVIDER_NOT_FOUND",()->federation.get(tenantB.id(),provider.id()));
        var domain=customization.addDomain(tenantA.id(),"login.example.com","dns");assertThatThrownBy(()->customization.deleteDomain(tenantB.id(),domain.id())).isInstanceOf(DomainException.class);
        UUID event=audit.record(new AuditService.Event(tenantA.id(),"admin",a,"phase6-a","security","update","security_policy",tenantA.id().toString(),"trace","PUT","127.0.0.1","test","success",null,Map.of("token","must-redact")));
        assertThat(audit.get(tenantA.id(),event).details()).containsEntry("token","[REDACTED]");assertThatThrownBy(()->audit.get(tenantB.id(),event)).isInstanceOf(DomainException.class);
    }

    private boolean createAfter(CountDownLatch start,UUID tenant,String username,String email)throws InterruptedException{start.await();try{users.create(tenant,input(username,email));return true;}catch(DomainException ex){assertThat(ex.code()).isEqualTo("TENANT_USER_LIMIT");return false;}}
    private static void assertCode(String code,ThrowingCallable action){assertThatThrownBy(action).isInstanceOf(DomainException.class).extracting("code").isEqualTo(code);}
    private static UserAccessCatalogService.RoleInput role(String name,String code,String scope,UUID parent,Map<String,Boolean> permissions){return new UserAccessCatalogService.RoleInput(name,code,null,null,permissions,scope,parent);}
    private static UserAccessCatalogService.PermissionInput permission(String code,String name,UUID parent){return new UserAccessCatalogService.PermissionInput(code,name,null,"operation",parent,"test","read");}
    private UUID account(String prefix){UUID id=UuidV7.randomUuid();db.sql("insert into admin_account(id,username,email,status) values(:id,:u,:e,'active')").param("id",id).param("u",prefix+id).param("e",prefix+id+"@example.com").update();return id;}
    private static PoolUserService.Input input(String username,String email){return new PoolUserService.Input(username,email,"Password1",null,username,null,null,null,null,null);}
}
