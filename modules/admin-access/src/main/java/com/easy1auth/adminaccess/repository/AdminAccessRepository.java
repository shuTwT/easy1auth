package com.easy1auth.adminaccess.repository;

import com.easy1auth.adminaccess.model.AdminRoleEntity;
import com.easy1auth.adminaccess.model.AdminRoleEntityFetcher;
import com.easy1auth.adminaccess.model.AdminRoleEntityProps;
import com.easy1auth.adminaccess.model.AdminRoleEntityTable;
import com.easy1auth.adminidentity.model.AdminAccountEntity;
import com.easy1auth.adminidentity.model.AdminAccountEntityTable;
import com.easy1auth.common.foundation.web.PageData;
import com.easy1auth.tenant.model.TenantMembershipEntity;
import com.easy1auth.tenant.model.TenantMembershipEntityTable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

/** 管理端角色与成员访问数据仓储。 */
public interface AdminAccessRepository extends JRepository<AdminRoleEntity, UUID> {
  AdminRoleEntityTable ROLE = AdminRoleEntityTable.$;
  AdminAccountEntityTable ACCOUNT = AdminAccountEntityTable.$;
  TenantMembershipEntityTable MEMBERSHIP = TenantMembershipEntityTable.$;

  default List<AdminRoleEntity> findRoles(String name, Boolean system) {
    return sql()
        .createQuery(ROLE)
        .whereIf(name != null, () -> ROLE.name().ilike(name, LikeMode.ANYWHERE))
        .whereIf(system != null, () -> ROLE.systemRole().eq(system))
        .orderBy(ROLE.createdAt().desc())
        .select(ROLE)
        .execute();
  }

  default PageData<AdminRoleEntity> pageRoles(int page, int pageSize, String name, Boolean system) {
    var query =
        sql()
            .createQuery(ROLE)
            .whereIf(name != null, () -> ROLE.name().ilike(name, LikeMode.ANYWHERE))
            .whereIf(system != null, () -> ROLE.systemRole().eq(system))
            .orderBy(ROLE.createdAt().desc())
            .select(
                ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().permissions().memberships()));
    return PageData.of(
        query.limit(pageSize, (long) (page - 1) * pageSize).execute(),
        page,
        pageSize,
        query.fetchUnlimitedCount());
  }

  default Optional<AdminRoleEntity> findRole(UUID id) {
    return sql().createQuery(ROLE).where(ROLE.id().eq(id)).select(ROLE).fetchOptional();
  }

  default Optional<AdminRoleEntity> findRole(UUID tenant, UUID id) {
    return sql()
        .createQuery(ROLE)
        .where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant))
        .select(ROLE)
        .fetchOptional();
  }

  default Optional<AdminRoleEntity> findRoleFetched(UUID id) {
    return sql()
        .createQuery(ROLE)
        .where(ROLE.id().eq(id))
        .select(ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().permissions().memberships()))
        .fetchOptional();
  }

  default Optional<AdminRoleEntity> findRoleFetched(UUID tenant, UUID id) {
    return sql()
        .createQuery(ROLE)
        .where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant))
        .select(ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().permissions().memberships()))
        .fetchOptional();
  }

  default Optional<AdminRoleEntity> findRoleWithPermissions(UUID id) {
    return sql()
        .createQuery(ROLE)
        .where(ROLE.id().eq(id))
        .select(ROLE.fetch(AdminRoleEntityFetcher.$.permissions()))
        .fetchOptional();
  }

  default void saveRole(AdminRoleEntity role) {
    sql().saveCommand(role).setMode(SaveMode.INSERT_ONLY).execute();
  }

  default int updateRole(UUID tenant, UUID id, String name, String description) {
    var update =
        sql()
            .createUpdate(ROLE)
            .set(ROLE.name(), name)
            .set(ROLE.updatedAt(), Instant.now())
            .where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant));
    if (description != null) {
      update.set(ROLE.description(), description);
    }
    return update.execute();
  }

  default long deleteRole(UUID id) {
    return sql().deleteById(AdminRoleEntity.class, id).getTotalAffectedRowCount();
  }

  default List<AdminAccountEntity> findAccounts() {
    return sql()
        .createQuery(ACCOUNT)
        .orderBy(ACCOUNT.createdAt().desc(), ACCOUNT.id().asc())
        .select(ACCOUNT)
        .execute();
  }

  default PageData<AdminAccountEntity> pageAccounts(
      int page, int pageSize, Set<UUID> allowed, String username, String email, String status) {
    var query =
        sql()
            .createQuery(ACCOUNT)
            .whereIf(allowed != null, () -> ACCOUNT.id().in(allowed))
            .whereIf(username != null, () -> ACCOUNT.username().ilike(username, LikeMode.ANYWHERE))
            .whereIf(email != null, () -> ACCOUNT.email().ilike(email, LikeMode.ANYWHERE))
            .whereIf(status != null, () -> ACCOUNT.status().eq(status))
            .orderBy(ACCOUNT.createdAt().desc(), ACCOUNT.id().asc())
            .select(ACCOUNT);
    return PageData.of(
        query.limit(pageSize, (long) (page - 1) * pageSize).execute(),
        page,
        pageSize,
        query.fetchUnlimitedCount());
  }

  default List<TenantMembershipEntity> findActiveMemberships() {
    return sql()
        .createQuery(MEMBERSHIP)
        .where(MEMBERSHIP.status().eq("active"))
        .orderBy(MEMBERSHIP.createdAt().asc(), MEMBERSHIP.tenantId().asc(), MEMBERSHIP.id().asc())
        .select(MEMBERSHIP)
        .execute();
  }

  default Optional<TenantMembershipEntity> findActiveMembership(UUID tenant, UUID account) {
    return sql()
        .createQuery(MEMBERSHIP)
        .where(
            MEMBERSHIP.tenantId().eq(tenant),
            MEMBERSHIP.accountId().eq(account),
            MEMBERSHIP.status().eq("active"))
        .select(MEMBERSHIP)
        .fetchOptional();
  }

  default List<TenantMembershipEntity> findActiveMemberships(UUID account) {
    return sql()
        .createQuery(MEMBERSHIP)
        .where(MEMBERSHIP.accountId().eq(account), MEMBERSHIP.status().eq("active"))
        .orderBy(MEMBERSHIP.createdAt().asc(), MEMBERSHIP.tenantId().asc(), MEMBERSHIP.id().asc())
        .select(MEMBERSHIP)
        .execute();
  }

  default List<UUID> findActiveMembershipIdsForUpdate(UUID account) {
    return sql()
        .createQuery(MEMBERSHIP)
        .where(MEMBERSHIP.accountId().eq(account), MEMBERSHIP.status().eq("active"))
        .select(MEMBERSHIP.id())
        .forUpdate()
        .execute();
  }

  default Optional<AdminAccountEntity> findAccount(UUID id) {
    return Optional.ofNullable(sql().findById(AdminAccountEntity.class, id));
  }

  default List<AdminRoleEntity> findRolesForMembership(UUID membership) {
    return sql()
        .createQuery(ROLE)
        .where(ROLE.memberships(m -> m.id().eq(membership)))
        .select(ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().permissions()))
        .execute();
  }

  default void replaceMembershipRoles(UUID membership, List<UUID> roleIds) {
    var associations = sql().getAssociations(AdminRoleEntityProps.MEMBERSHIPS);
    findRolesForMembership(membership).forEach(role -> associations.delete(role.id(), membership));
    roleIds.forEach(roleId -> associations.insert(roleId, membership));
  }

  default void replacePermissions(UUID role, List<String> codes) {
    var associations = sql().getAssociations(AdminRoleEntityProps.PERMISSIONS);
    findRoleWithPermissions(role)
        .orElseThrow()
        .permissions()
        .forEach(permission -> associations.delete(role, permission.code()));
    codes.forEach(code -> associations.insert(role, code));
  }

  default boolean roleExists(UUID tenant, String name) {
    return sql()
        .createQuery(ROLE)
        .where(ROLE.tenantId().eq(tenant), ROLE.name().eq(name))
        .select(ROLE.id())
        .exists();
  }

  default List<AdminRoleEntity> findRolesWithMemberships() {
    return sql()
        .createQuery(ROLE)
        .select(ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().memberships()))
        .execute();
  }
}
