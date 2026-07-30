package com.easy1auth.tenant;

import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.tenant.model.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;

@Repository
class TenantRepository {
    private static final TenantEntityTable TENANT = TenantEntityTable.$;
    private static final TenantMembershipEntityTable MEMBERSHIP = TenantMembershipEntityTable.$;
    private final JSqlClient sql;
    TenantRepository(JSqlClient sql) { this.sql = sql; }

    List<TenantState> listForAccount(UUID accountId) {
        var memberships=sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(accountId),MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).execute();
        if(memberships.isEmpty())return List.of();
        var byTenant=new HashMap<UUID,String>(); memberships.forEach(m->byTenant.put(m.tenantId(),m.membershipRole()));
        return sql.createQuery(TENANT).where(TENANT.id().in(byTenant.keySet())).orderBy(TENANT.createdAt().desc()).select(TENANT.fetch(TenantEntityFetcher.$.allScalarFields().packageInfo(TenantPackageEntityFetcher.$.allScalarFields()))).execute().stream().map(t->new TenantState(t,byTenant.get(t.id()))).toList();
    }
    Optional<MembershipState> activeMembership(UUID accountId,UUID tenantId){var membership=sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(accountId),MEMBERSHIP.tenantId().eq(tenantId),MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).fetchOptional();if(membership.isEmpty())return Optional.empty();var tenant=sql.createQuery(TENANT).where(TENANT.id().eq(tenantId),TENANT.status().eq("active")).select(TENANT.fetch(TenantEntityFetcher.$.allScalarFields().packageInfo(TenantPackageEntityFetcher.$.allScalarFields()))).fetchOptional();if(tenant.isEmpty())return Optional.empty();var tenantPackage=tenant.get().packageInfo();return Optional.of(new MembershipState(membership.get().id(),membership.get().membershipRole(),tenant.get().isSystem(),tenantPackage==null?null:tenantPackage.id()));}
    Optional<TenantEntity> lockActiveTenant(UUID tenantId){return sql.createQuery(TENANT).where(TENANT.id().eq(tenantId),TENANT.status().eq("active")).select(TENANT).forUpdate().fetchOptional();}
    Optional<TenantMembershipEntity> lockActiveMembership(UUID tenantId){return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.tenantId().eq(tenantId),MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).forUpdate().fetchOptional();}
    Optional<TenantMembershipEntity> lockMembership(UUID tenantId,UUID accountId){return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.tenantId().eq(tenantId),MEMBERSHIP.accountId().eq(accountId)).select(MEMBERSHIP).forUpdate().fetchOptional();}
    TenantEntity createOrdinaryTenant(String name,long packageId){Instant now=Instant.now();var tenant=TenantEntityDraft.$.produce(draft->draft.setId(UuidV7.randomUuid()).setName(name).setStatus("active").setSystem(false).setPackageInfo(TenantPackageEntityDraft.$.produce(tenantPackage->tenantPackage.setId(packageId))).setCreatedAt(now).setUpdatedAt(now));sql.saveCommand(tenant).setMode(SaveMode.INSERT_ONLY).execute();return tenant;}
    void createActiveAdministratorMembership(UUID tenantId,UUID accountId,String role){Instant now=Instant.now();sql.saveCommand(TenantMembershipEntityDraft.$.produce(draft->draft.setId(UuidV7.randomUuid()).setTenantId(tenantId).setAccountId(accountId).setMembershipRole(role).setStatus("active").setCreatedAt(now).setUpdatedAt(now))).setMode(SaveMode.INSERT_ONLY).execute();}
    void suspendMembership(UUID membershipId){sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.status(),"suspended").set(MEMBERSHIP.updatedAt(),Instant.now()).where(MEMBERSHIP.id().eq(membershipId),MEMBERSHIP.status().eq("active")).execute();}
    void activateAdministratorMembership(UUID membershipId,String role){sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.membershipRole(),role).set(MEMBERSHIP.status(),"active").set(MEMBERSHIP.updatedAt(),Instant.now()).where(MEMBERSHIP.id().eq(membershipId),MEMBERSHIP.status().eq("suspended")).execute();}
    int lockAndGetMaxUsers(UUID tenantId){return sql.createQuery(TENANT).where(TENANT.id().eq(tenantId),TENANT.status().eq("active"),TENANT.system().eq(false),TENANT.packageInfo().status().eq("active")).select(TENANT.packageInfo().maxUsers()).forUpdate().fetchOne();}
    int lockAndGetMaxApps(UUID tenantId){return sql.createQuery(TENANT).where(TENANT.id().eq(tenantId),TENANT.status().eq("active"),TENANT.system().eq(false),TENANT.packageInfo().status().eq("active")).select(TENANT.packageInfo().maxApps()).forUpdate().fetchOne();}
    record MembershipState(UUID id,String role,boolean system,Long packageId){}
    record TenantState(TenantEntity tenant,String role){}
}
