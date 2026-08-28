package com.easy1auth.poolidentity.repository;

import com.easy1auth.poolidentity.model.PoolPermissionEntity;
import com.easy1auth.poolidentity.model.PoolPermissionEntityTable;
import com.easy1auth.poolidentity.model.PoolRoleEntity;
import com.easy1auth.poolidentity.model.PoolRoleEntityTable;
import com.easy1auth.poolidentity.model.PoolUserEntity;
import com.easy1auth.poolidentity.model.PoolUserEntityTable;
import com.easy1auth.poolidentity.model.UserRoleAssignmentEntity;
import com.easy1auth.poolidentity.model.UserRoleAssignmentEntityTable;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户访问控制目录仓储。
 *
 * <p>基于 Jimmer {@link JRepository} 封装角色、权限、用户角色分配及相关用户的持久化操作。
 * Service 只负责领域校验、树结构组装和事务编排。</p>
 */
public interface UserAccessCatalogRepository extends JRepository<PoolRoleEntity, UUID> {
    PoolRoleEntityTable ROLE = PoolRoleEntityTable.$;
    PoolPermissionEntityTable PERMISSION = PoolPermissionEntityTable.$;
    UserRoleAssignmentEntityTable ASSIGNMENT = UserRoleAssignmentEntityTable.$;
    PoolUserEntityTable USER = PoolUserEntityTable.$;

    default List<PoolRoleEntity> findRoles(UUID tenant, String search, String type) {
        return sql().createQuery(ROLE).where(ROLE.tenantId().eq(tenant))
                .whereIf(search != null, () -> Predicate.or(ROLE.name().ilike(search, LikeMode.ANYWHERE), ROLE.code().ilike(search, LikeMode.ANYWHERE)))
                .whereIf(type != null, () -> ROLE.type().eq(type)).orderBy(ROLE.createdAt().desc()).select(ROLE).execute();
    }

    default Optional<PoolRoleEntity> findRole(UUID tenant, UUID id) {
        return sql().createQuery(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).select(ROLE).fetchOptional();
    }

    default List<PoolRoleEntity> findRolesByIds(UUID tenant, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : sql().createQuery(ROLE).where(ROLE.tenantId().eq(tenant), ROLE.id().in(ids)).select(ROLE).execute();
    }

    default List<PoolRoleEntity> findAllRoles(UUID tenant) {
        return sql().createQuery(ROLE).where(ROLE.tenantId().eq(tenant)).orderBy(ROLE.createdAt()).select(ROLE).execute();
    }

    default boolean hasRoleChildren(UUID tenant, UUID id) {
        return sql().createQuery(ROLE).where(ROLE.tenantId().eq(tenant), ROLE.parentId().eq(id)).select(ROLE.id()).exists();
    }

    default void saveRole(PoolRoleEntity role) {
        sql().saveCommand(role).setMode(SaveMode.INSERT_ONLY).execute();
    }

    default void updateRole(UUID tenant, UUID id, String name, String scope, String description, Map<String, Boolean> permissions, UUID parentId) {
        var update = sql().createUpdate(ROLE).set(ROLE.name(), name).set(ROLE.dataScope(), scope).set(ROLE.updatedAt(), Instant.now()).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant));
        if (description != null) update.set(ROLE.description(), description);
        if (permissions != null) update.set(ROLE.permissions(), permissions);
        if (parentId != null) update.set(ROLE.parentId(), parentId);
        update.execute();
    }

    default void deleteRole(UUID tenant, UUID id) {
        sql().createDelete(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).execute();
    }

    default long assignmentCount(UUID tenant, UUID roleId) {
        return sql().createQuery(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().roleId().eq(roleId)).select(ASSIGNMENT.id()).fetchUnlimitedCount();
    }

    default List<UUID> findAssignedUserIds(UUID tenant, UUID roleId) {
        return sql().createQuery(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().roleId().eq(roleId)).select(ASSIGNMENT.id().userId()).execute();
    }

    default List<UUID> findAssignedRoleIds(UUID tenant, UUID userId) {
        return sql().createQuery(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().userId().eq(userId)).select(ASSIGNMENT.id().roleId()).execute();
    }

    default List<PoolUserEntity> findUsers(UUID tenant, Collection<UUID> ids, String search) {
        return ids.isEmpty() ? List.of() : sql().createQuery(USER).where(USER.tenantId().eq(tenant), USER.id().in(ids))
                .whereIf(search != null, () -> Predicate.or(USER.username().ilike(search, LikeMode.ANYWHERE), USER.email().ilike(search, LikeMode.ANYWHERE), USER.name().ilike(search, LikeMode.ANYWHERE)))
                .orderBy(USER.createdAt().desc()).select(USER).execute();
    }

    default Optional<PoolUserEntity> findUser(UUID tenant, UUID id) {
        return sql().createQuery(USER).where(USER.id().eq(id), USER.tenantId().eq(tenant)).select(USER).fetchOptional();
    }

    default void saveAssignment(UserRoleAssignmentEntity assignment) {
        sql().saveCommand(assignment).setMode(SaveMode.INSERT_IF_ABSENT).execute();
    }

    default void deleteAssignment(UUID tenant, UUID userId, UUID roleId) {
        sql().createDelete(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().userId().eq(userId), ASSIGNMENT.id().roleId().eq(roleId)).execute();
    }

    default void deleteAssignmentsForUser(UUID tenant, UUID userId) {
        sql().createDelete(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().userId().eq(userId)).execute();
    }

    default List<PoolPermissionEntity> findPermissions(UUID tenant, String search, String type, String resource) {
        return sql().createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant))
                .whereIf(search != null && !search.isBlank(), () -> Predicate.or(PERMISSION.name().ilike(search, LikeMode.ANYWHERE), PERMISSION.code().ilike(search, LikeMode.ANYWHERE), PERMISSION.resource().ilike(search, LikeMode.ANYWHERE)))
                .whereIf(type != null && !type.isBlank(), () -> PERMISSION.type().eq(type)).whereIf(resource != null && !resource.isBlank(), () -> PERMISSION.resource().eq(resource))
                .orderBy(PERMISSION.resource(), PERMISSION.code()).select(PERMISSION).execute();
    }

    default List<PoolPermissionEntity> findAllPermissions(UUID tenant) {
        return sql().createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant)).orderBy(PERMISSION.type(), PERMISSION.resource(), PERMISSION.code()).select(PERMISSION).execute();
    }

    default List<String> findPermissionTypes(UUID tenant) {
        return sql().createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant)).select(PERMISSION.type()).execute();
    }

    default Optional<PoolPermissionEntity> findPermission(UUID tenant, UUID id) {
        return sql().createQuery(PERMISSION).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant)).select(PERMISSION).fetchOptional();
    }

    default boolean hasPermissionChildren(UUID tenant, UUID id) {
        return sql().createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant), PERMISSION.parentId().eq(id)).select(PERMISSION.id()).exists();
    }

    default void savePermission(PoolPermissionEntity permission) {
        sql().saveCommand(permission).setMode(SaveMode.INSERT_ONLY).execute();
    }

    default void updatePermission(UUID tenant, UUID id, String name, String type, String resource, String action, String description, UUID parentId) {
        var update = sql().createUpdate(PERMISSION).set(PERMISSION.name(), name).set(PERMISSION.type(), type).set(PERMISSION.resource(), resource).set(PERMISSION.action(), action).set(PERMISSION.updatedAt(), Instant.now()).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant));
        if (description != null) update.set(PERMISSION.description(), description);
        if (parentId != null) update.set(PERMISSION.parentId(), parentId);
        update.execute();
    }

    default void deletePermission(UUID tenant, UUID id) {
        sql().createDelete(PERMISSION).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant)).execute();
    }

    default List<String> findPermissionCodes(UUID tenant, Collection<String> codes) {
        return codes.isEmpty() ? List.of() : sql().createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant), PERMISSION.code().in(codes)).select(PERMISSION.code()).execute();
    }
}
