package com.easy1auth.adminaccess.repository;

import com.easy1auth.adminaccess.model.AdminRoleEntity;
import com.easy1auth.adminaccess.model.AdminRoleEntityTable;
import com.easy1auth.adminidentity.model.AdminAccountEntity;
import com.easy1auth.adminidentity.model.AdminAccountEntityTable;
import com.easy1auth.tenant.model.TenantMembershipEntity;
import com.easy1auth.tenant.model.TenantMembershipEntityTable;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 管理端角色与成员访问数据仓储。 */
public interface AdminAccessRepository extends JRepository<AdminRoleEntity, UUID> {
    AdminRoleEntityTable ROLE = AdminRoleEntityTable.$;
    AdminAccountEntityTable ACCOUNT = AdminAccountEntityTable.$;
    TenantMembershipEntityTable MEMBERSHIP = TenantMembershipEntityTable.$;

    default List<AdminRoleEntity> findRoles(String name, Boolean system) {
        return sql().createQuery(ROLE)
                .whereIf(name != null, () -> ROLE.name().ilike(name, LikeMode.ANYWHERE))
                .whereIf(system != null, () -> ROLE.systemRole().eq(system))
                .orderBy(ROLE.createdAt().desc())
                .select(ROLE).execute();
    }

    default Optional<AdminRoleEntity> findRole(UUID id) {
        return sql().createQuery(ROLE).where(ROLE.id().eq(id)).select(ROLE).fetchOptional();
    }

    default Optional<AdminRoleEntity> findRole(UUID tenant, UUID id) {
        return sql().createQuery(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).select(ROLE).fetchOptional();
    }

    default void saveRole(AdminRoleEntity role) {
        sql().saveCommand(role).setMode(SaveMode.INSERT_ONLY).execute();
    }

    default int updateRole(UUID tenant, UUID id, String name, String description) {
        var update = sql().createUpdate(ROLE).set(ROLE.name(), name).set(ROLE.updatedAt(), Instant.now()).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant));
        if (description != null) update.set(ROLE.description(), description);
        return update.execute();
    }

    default long deleteRole(UUID id) {
        return sql().deleteById(AdminRoleEntity.class, id).getTotalAffectedRowCount();
    }

    default List<AdminAccountEntity> findAccounts() {
        return sql().createQuery(ACCOUNT).orderBy(ACCOUNT.createdAt().desc(), ACCOUNT.id().asc()).select(ACCOUNT).execute();
    }

    default List<TenantMembershipEntity> findActiveMemberships() {
        return sql().createQuery(MEMBERSHIP).where(MEMBERSHIP.status().eq("active"))
                .orderBy(MEMBERSHIP.createdAt().asc(), MEMBERSHIP.tenantId().asc(), MEMBERSHIP.id().asc()).select(MEMBERSHIP).execute();
    }

    default Optional<TenantMembershipEntity> findActiveMembership(UUID tenant, UUID account) {
        return sql().createQuery(MEMBERSHIP).where(MEMBERSHIP.tenantId().eq(tenant), MEMBERSHIP.accountId().eq(account), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).fetchOptional();
    }
}
