package com.easy1auth.connection.repository;

import com.easy1auth.connection.dto.EnterpriseIdentityInput;
import com.easy1auth.connection.model.*;
import com.easy1auth.common.foundation.web.PageData;
import com.easy1auth.connection.model.EnterpriseIdentitySourceEntity;
import com.easy1auth.connection.model.EnterpriseIdentitySourceEntityTable;
import com.easy1auth.connection.model.EnterpriseIdentitySyncTaskEntity;
import com.easy1auth.connection.model.EnterpriseIdentitySyncTaskEntityTable;
import com.easy1auth.poolidentity.model.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

/** 企业身份源及同步数据访问仓储。 */
public interface EnterpriseIdentityRepository
    extends JRepository<EnterpriseIdentitySourceEntity, UUID> {
  EnterpriseIdentitySourceEntityTable SOURCE = EnterpriseIdentitySourceEntityTable.$;
  EnterpriseIdentitySyncTaskEntityTable TASK = EnterpriseIdentitySyncTaskEntityTable.$;
  PoolUserEntityTable USER = PoolUserEntityTable.$;
  UserGroupEntityTable GROUP = UserGroupEntityTable.$;
  UserGroupAssignmentEntityTable MEMBERSHIP = UserGroupAssignmentEntityTable.$;

  default PageData<EnterpriseIdentitySourceEntity> pageSources(
      UUID tenant, int page, int size, String search, String status) {
    var q =
        sql()
            .createQuery(SOURCE)
            .where(SOURCE.tenantId().eq(tenant))
            .whereIf(
                search != null && !search.isBlank(),
                () -> SOURCE.name().ilike(search, LikeMode.ANYWHERE))
            .whereIf(status != null && !status.isBlank(), () -> SOURCE.status().eq(status))
            .orderBy(SOURCE.createdAt().desc())
            .select(SOURCE);
    return PageData.of(
        q.limit(size, (long) (page - 1) * size).execute(), page, size, q.fetchUnlimitedCount());
  }

  default void saveSource(EnterpriseIdentitySourceEntity source) {
    sql().saveCommand(source).setMode(SaveMode.INSERT_ONLY).execute();
  }

  default int updateSource(
      UUID tenant,
      UUID id,
      EnterpriseIdentityInput input,
      String appSecret,
      String verificationToken,
      String encryptKey,
      String status) {
    var update =
        sql()
            .createUpdate(SOURCE)
            .set(SOURCE.updatedAt(), Instant.now())
            .where(SOURCE.id().eq(id), SOURCE.tenantId().eq(tenant));
    if (input.name() != null) {
      update.set(SOURCE.name(), input.name().strip());
    }
    if (input.appId() != null) {
      update.set(SOURCE.appId(), input.appId().strip());
    }
    if (appSecret != null) {
      update.set(SOURCE.encryptedAppSecret(), appSecret);
    }
    if (verificationToken != null) {
      update.set(SOURCE.encryptedVerificationToken(), verificationToken);
    }
    if (encryptKey != null) {
      update.set(SOURCE.encryptedEncryptKey(), encryptKey);
    }
    if (status != null) {
      update.set(SOURCE.status(), status);
    }
    return update.execute();
  }

  default void deleteSourceAndDetachImportedData(UUID tenant, UUID id) {
    sql()
        .createUpdate(USER)
        .set(USER.enterpriseIdentitySourceId(), (UUID) null)
        .set(USER.enterpriseIdentityExternalId(), (String) null)
        .where(USER.tenantId().eq(tenant), USER.enterpriseIdentitySourceId().eq(id))
        .execute();
    sql()
        .createUpdate(GROUP)
        .set(GROUP.enterpriseIdentitySourceId(), (UUID) null)
        .set(GROUP.enterpriseIdentityExternalId(), (String) null)
        .where(GROUP.tenantId().eq(tenant), GROUP.enterpriseIdentitySourceId().eq(id))
        .execute();
    sql().createDelete(SOURCE).where(SOURCE.id().eq(id), SOURCE.tenantId().eq(tenant)).execute();
  }

  default List<EnterpriseIdentitySyncTaskEntity> findRecentTasks(UUID tenant, UUID source) {
    return sql()
        .createQuery(TASK)
        .where(TASK.tenantId().eq(tenant), TASK.sourceId().eq(source))
        .orderBy(TASK.createdAt().desc())
        .select(TASK)
        .limit(30)
        .execute();
  }

  default List<String> findSourceStatuses(UUID tenant) {
    return sql()
        .createQuery(SOURCE)
        .where(SOURCE.tenantId().eq(tenant))
        .select(SOURCE.status())
        .execute();
  }

  default EnterpriseIdentitySourceEntity findSource(UUID id) {
    return sql().findById(EnterpriseIdentitySourceEntity.class, id);
  }

  default List<EnterpriseIdentitySyncTaskEntity> findPendingTasks(int limit) {
    return sql()
        .createQuery(TASK)
        .where(TASK.status().eq("pending"))
        .orderBy(TASK.createdAt().asc())
        .select(TASK)
        .limit(Math.max(1, Math.min(limit, 20)))
        .execute();
  }

  default boolean claimTask(UUID id) {
    return sql()
            .createUpdate(TASK)
            .set(TASK.status(), "processing")
            .set(TASK.startedAt(), Instant.now())
            .where(TASK.id().eq(id), TASK.status().eq("pending"))
            .execute()
        == 1;
  }

  default EnterpriseIdentitySyncTaskEntity findTask(UUID id) {
    return sql().findById(EnterpriseIdentitySyncTaskEntity.class, id);
  }

  default Optional<UserGroupEntity> findGroup(UUID tenant, UUID source, String externalId) {
    return Optional.ofNullable(
        sql()
            .createQuery(GROUP)
            .where(
                GROUP.tenantId().eq(tenant),
                GROUP.enterpriseIdentitySourceId().eq(source),
                GROUP.enterpriseIdentityExternalId().eq(externalId))
            .select(GROUP)
            .fetchOneOrNull());
  }

  default void saveGroup(UserGroupEntity group) {
    sql().saveCommand(group).setMode(SaveMode.INSERT_ONLY).execute();
  }

  default void updateGroup(UUID id, String name, UUID parent) {
    sql()
        .createUpdate(GROUP)
        .set(GROUP.name(), name)
        .set(GROUP.parentId(), parent)
        .set(GROUP.updatedAt(), Instant.now())
        .where(GROUP.id().eq(id))
        .execute();
  }

  default Optional<PoolUserEntity> findUser(UUID tenant, UUID source, String externalId) {
    return Optional.ofNullable(
        sql()
            .createQuery(USER)
            .where(
                USER.tenantId().eq(tenant),
                USER.enterpriseIdentitySourceId().eq(source),
                USER.enterpriseIdentityExternalId().eq(externalId))
            .select(USER)
            .fetchOneOrNull());
  }

  default boolean userEmailExists(UUID tenant, String email) {
    return sql()
        .createQuery(USER)
        .where(USER.tenantId().eq(tenant), USER.email().eq(email))
        .select(USER.id())
        .exists();
  }

  default boolean userPhoneExists(UUID tenant, String phone) {
    return sql()
        .createQuery(USER)
        .where(USER.tenantId().eq(tenant), USER.phone().eq(phone))
        .select(USER.id())
        .exists();
  }

  default Optional<UserGroupEntity> findSourceGroup(UUID source, String externalId) {
    return Optional.ofNullable(
        sql()
            .createQuery(GROUP)
            .where(
                GROUP.enterpriseIdentitySourceId().eq(source),
                GROUP.enterpriseIdentityExternalId().eq(externalId))
            .select(GROUP)
            .fetchOneOrNull());
  }

  default void linkUser(UUID id, UUID source, String externalId) {
    sql()
        .createUpdate(USER)
        .set(USER.enterpriseIdentitySourceId(), source)
        .set(USER.enterpriseIdentityExternalId(), externalId)
        .where(USER.id().eq(id))
        .execute();
  }

  default void updateUser(
      UUID id,
      String email,
      String name,
      String phone,
      String avatar,
      String department,
      String position) {
    sql()
        .createUpdate(USER)
        .set(USER.email(), email)
        .set(USER.name(), name)
        .set(USER.phone(), phone)
        .set(USER.avatar(), avatar)
        .set(USER.department(), department)
        .set(USER.position(), position)
        .set(USER.status(), "active")
        .set(USER.updatedAt(), Instant.now())
        .where(USER.id().eq(id))
        .execute();
  }

  default Optional<PoolUserEntity> findUser(UUID source, String externalId) {
    return Optional.ofNullable(
        sql()
            .createQuery(USER)
            .where(
                USER.enterpriseIdentitySourceId().eq(source),
                USER.enterpriseIdentityExternalId().eq(externalId))
            .select(USER)
            .fetchOneOrNull());
  }

  default List<UserGroupEntity> findSourceGroups(UUID source) {
    return sql()
        .createQuery(GROUP)
        .where(GROUP.enterpriseIdentitySourceId().eq(source))
        .select(GROUP)
        .execute();
  }

  default void replaceUserGroups(
      UUID tenant, UUID user, List<UUID> sourceGroups, Set<UUID> assignedGroups) {
    if (!sourceGroups.isEmpty()) {
      sql()
          .createDelete(MEMBERSHIP)
          .where(
              MEMBERSHIP.id().tenantId().eq(tenant),
              MEMBERSHIP.id().userId().eq(user),
              MEMBERSHIP.id().groupId().in(sourceGroups))
          .execute();
    }
    assignedGroups.forEach(
        groupId ->
            sql()
                .saveCommand(
                    UserGroupAssignmentEntityDraft.$.produce(
                        d ->
                            d.setId(
                                UserGroupAssignmentIdDraft.$.produce(
                                    k ->
                                        k.setTenantId(tenant)
                                            .setUserId(user)
                                            .setGroupId(groupId)))))
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .execute());
  }

  default void disableUser(UUID source, String externalId) {
    sql()
        .createUpdate(USER)
        .set(USER.status(), "disabled")
        .set(USER.updatedAt(), Instant.now())
        .where(
            USER.enterpriseIdentitySourceId().eq(source),
            USER.enterpriseIdentityExternalId().eq(externalId))
        .execute();
  }

  default void saveTask(EnterpriseIdentitySyncTaskEntity task) {
    sql().saveCommand(task).setMode(SaveMode.INSERT_ONLY).execute();
  }

  default void finishTask(UUID id, Map<String, Object> summary, String result, String error) {
    sql()
        .createUpdate(TASK)
        .set(TASK.status(), result)
        .set(TASK.summary(), summary)
        .set(TASK.lastError(), error)
        .set(TASK.finishedAt(), Instant.now())
        .where(TASK.id().eq(id))
        .execute();
    var task = findTask(id);
    if (task != null) {
      sql()
          .createUpdate(SOURCE)
          .set(SOURCE.lastSyncAt(), Instant.now())
          .set(SOURCE.lastSyncStatus(), result)
          .set(SOURCE.lastError(), error)
          .set(SOURCE.updatedAt(), Instant.now())
          .where(SOURCE.id().eq(task.sourceId()))
          .execute();
    }
  }

  default Optional<EnterpriseIdentitySourceEntity> findSource(UUID tenant, UUID id) {
    return sql()
        .createQuery(SOURCE)
        .where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id))
        .select(SOURCE)
        .fetchOptional();
  }

  default boolean groupNameExists(UUID tenant, UUID self, UUID parent, String name) {
    return sql()
        .createQuery(GROUP)
        .where(GROUP.tenantId().eq(tenant), GROUP.parentId().eq(parent), GROUP.name().eq(name))
        .whereIf(self != null, () -> GROUP.id().ne(self))
        .select(GROUP.id())
        .exists();
  }
}
