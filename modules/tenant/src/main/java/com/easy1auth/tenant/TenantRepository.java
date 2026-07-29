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

    TenantSummary createOwned(UUID accountId, String name, String plan) {
        UUID tenantId=UuidV7.randomUuid(); Instant now=Instant.now();
        sql.saveCommand(TenantEntityDraft.$.produce(d->d.setId(tenantId).setName(name).setStatus("active").setPlan(plan).setMaxUsers(100).setMaxApps(10).setCreatedAt(now).setUpdatedAt(now))).setMode(SaveMode.INSERT_ONLY).execute();
        sql.saveCommand(TenantMembershipEntityDraft.$.produce(d->d.setId(UuidV7.randomUuid()).setAccountId(accountId).setTenantId(tenantId).setMembershipRole("owner").setStatus("active").setCreatedAt(now).setUpdatedAt(now))).setMode(SaveMode.INSERT_ONLY).execute();
        return new TenantSummary(tenantId,name,"active",plan,"owner");
    }
    List<TenantSummary> listForAccount(UUID accountId) {
        var memberships=sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(accountId),MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).execute();
        if(memberships.isEmpty())return List.of();
        var byTenant=new HashMap<UUID,String>(); memberships.forEach(m->byTenant.put(m.tenantId(),m.membershipRole()));
        return sql.createQuery(TENANT).where(TENANT.id().in(byTenant.keySet())).orderBy(TENANT.createdAt().desc()).select(TENANT).execute().stream().map(t->new TenantSummary(t.id(),t.name(),t.status(),t.plan(),byTenant.get(t.id()))).toList();
    }
    Optional<MembershipState> activeMembership(UUID accountId,UUID tenantId){var membership=sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(accountId),MEMBERSHIP.tenantId().eq(tenantId),MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).fetchOptional();if(membership.isEmpty())return Optional.empty();var tenant=sql.createQuery(TENANT).where(TENANT.id().eq(tenantId),TENANT.status().eq("active")).select(TENANT.id()).fetchOptional();return tenant.isEmpty()?Optional.empty():Optional.of(new MembershipState(membership.get().id(),membership.get().membershipRole()));}
    void lockTenant(UUID tenantId){sql.createQuery(TENANT).where(TENANT.id().eq(tenantId)).select(TENANT.id()).forUpdate().fetchOne();}
    String role(UUID accountId,UUID tenantId){return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(accountId),MEMBERSHIP.tenantId().eq(tenantId),MEMBERSHIP.status().eq("active")).select(MEMBERSHIP.membershipRole()).fetchOneOrNull();}
    void transfer(UUID tenantId,UUID from,UUID to){sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.membershipRole(),"member").set(MEMBERSHIP.updatedAt(),Instant.now()).where(MEMBERSHIP.tenantId().eq(tenantId),MEMBERSHIP.accountId().eq(from),MEMBERSHIP.membershipRole().eq("owner")).execute();int changed=sql.createUpdate(MEMBERSHIP).set(MEMBERSHIP.membershipRole(),"owner").set(MEMBERSHIP.updatedAt(),Instant.now()).where(MEMBERSHIP.tenantId().eq(tenantId),MEMBERSHIP.accountId().eq(to),MEMBERSHIP.status().eq("active")).execute();if(changed!=1)throw new IllegalStateException("target membership missing");}
    int remove(UUID tenantId,UUID accountId){return sql.createDelete(MEMBERSHIP).where(MEMBERSHIP.tenantId().eq(tenantId),MEMBERSHIP.accountId().eq(accountId),MEMBERSHIP.membershipRole().ne("owner")).execute();}
    boolean isOwnerAnywhere(UUID accountId){return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(accountId),MEMBERSHIP.membershipRole().eq("owner"),MEMBERSHIP.status().eq("active")).select(MEMBERSHIP.id()).exists();}
    int lockAndGetMaxUsers(UUID tenantId){return sql.createQuery(TENANT).where(TENANT.id().eq(tenantId),TENANT.status().eq("active")).select(TENANT.maxUsers()).forUpdate().fetchOne();}
    int lockAndGetMaxApps(UUID tenantId){return sql.createQuery(TENANT).where(TENANT.id().eq(tenantId),TENANT.status().eq("active")).select(TENANT.maxApps()).forUpdate().fetchOne();}
    record MembershipState(UUID id,String role){}
}
