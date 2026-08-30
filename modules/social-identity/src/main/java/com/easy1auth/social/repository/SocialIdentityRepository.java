package com.easy1auth.social.repository;

import com.easy1auth.common.foundation.web.PageData;
import com.easy1auth.social.dto.SocialIdentityInput;
import com.easy1auth.social.model.SocialIdentityBindingEntity;
import com.easy1auth.social.model.SocialIdentityBindingEntityDraft;
import com.easy1auth.social.model.SocialIdentityBindingEntityTable;
import com.easy1auth.social.model.SocialIdentitySourceEntity;
import com.easy1auth.social.model.SocialIdentitySourceEntityDraft;
import com.easy1auth.social.model.SocialIdentitySourceEntityTable;
import com.easy1auth.social.model.SocialLoginTransactionEntity;
import com.easy1auth.social.model.SocialLoginTransactionEntityDraft;
import com.easy1auth.social.model.SocialLoginTransactionEntityTable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

/** 社会化身份源及登录事务数据仓储。 */
public interface SocialIdentityRepository extends JRepository<SocialIdentitySourceEntity, UUID> {
  SocialIdentitySourceEntityTable SOURCE = SocialIdentitySourceEntityTable.$;
  SocialLoginTransactionEntityTable TX = SocialLoginTransactionEntityTable.$;
  SocialIdentityBindingEntityTable BINDING = SocialIdentityBindingEntityTable.$;

  default SocialIdentitySourceEntity createSource(
      UUID id,
      UUID tenant,
      String name,
      String type,
      String mode,
      String clientId,
      String encryptedSecret,
      boolean jit) {
    Instant now = Instant.now();
    var entity =
        SocialIdentitySourceEntityDraft.$.produce(
            draft ->
                draft
                    .setId(id)
                    .setTenantId(tenant)
                    .setName(name)
                    .setType(type)
                    .setMode(mode)
                    .setClientId(clientId)
                    .setEncryptedClientSecret(encryptedSecret)
                    .setJitProvisioning(jit)
                    .setStatus("active")
                    .setCreatedAt(now)
                    .setUpdatedAt(now));
    sql().saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
    return entity;
  }

  default SocialLoginTransactionEntity createTransaction(
      UUID id,
      UUID tenant,
      UUID source,
      String stateHash,
      String nonceHash,
      String encryptedNonce,
      String encryptedPkce,
      String returnUri,
      Instant expires) {
    var entity =
        SocialLoginTransactionEntityDraft.$.produce(
            draft ->
                draft
                    .setId(id)
                    .setTenantId(tenant)
                    .setSourceId(source)
                    .setStateHash(stateHash)
                    .setNonceHash(nonceHash)
                    .setEncryptedNonce(encryptedNonce)
                    .setEncryptedPkceVerifier(encryptedPkce)
                    .setReturnUri(returnUri)
                    .setExpiresAt(expires)
                    .setConsumedAt(null)
                    .setCreatedAt(Instant.now()));
    sql().saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
    return entity;
  }

  default void createBinding(
      UUID tenant,
      UUID source,
      UUID user,
      String sourceType,
      String subject,
      Map<String, Object> claims) {
    var entity =
        SocialIdentityBindingEntityDraft.$.produce(
            draft ->
                draft
                    .setId(UUID.randomUUID())
                    .setTenantId(tenant)
                    .setSourceId(source)
                    .setPoolUserId(user)
                    .setSourceType(sourceType)
                    .setSubject(subject)
                    .setClaims(claims)
                    .setCreatedAt(Instant.now())
                    .setLastLoginAt(Instant.now()));
    sql().saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
  }

  default PageData<SocialIdentitySourceEntity> page(
      UUID tenant, int page, int size, String search, String status) {
    var query =
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
        query.limit(size, (long) (page - 1) * size).execute(),
        page,
        size,
        query.fetchUnlimitedCount());
  }

  default Optional<SocialIdentitySourceEntity> findSource(UUID tenant, UUID id) {
    return sql()
        .createQuery(SOURCE)
        .where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id))
        .select(SOURCE)
        .fetchOptional();
  }

  default int updateSource(
      UUID tenant, UUID id, SocialIdentityInput input, String encryptedSecret, String status) {
    var update =
        sql()
            .createUpdate(SOURCE)
            .set(SOURCE.updatedAt(), Instant.now())
            .where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id));
    if (input.name() != null) {
      update.set(SOURCE.name(), input.name().strip());
    }
    if (input.type() != null) {
      update.set(SOURCE.type(), input.type());
    }
    if (input.mode() != null) {
      update.set(SOURCE.mode(), input.mode());
    }
    if (input.clientId() != null) {
      update.set(SOURCE.clientId(), input.clientId().strip());
    }
    if (encryptedSecret != null) {
      update.set(SOURCE.encryptedClientSecret(), encryptedSecret);
    }
    if (input.jitProvisioning() != null) {
      update.set(SOURCE.jitProvisioning(), input.jitProvisioning());
    }
    if (status != null) {
      update.set(SOURCE.status(), status);
    }
    return update.execute();
  }

  default int deleteSource(UUID tenant, UUID id) {
    return sql()
        .createDelete(SOURCE)
        .where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id))
        .execute();
  }

  default List<SocialIdentitySourceEntity> findActiveSources(UUID tenant) {
    return sql()
        .createQuery(SOURCE)
        .where(SOURCE.tenantId().eq(tenant), SOURCE.status().eq("active"))
        .orderBy(SOURCE.createdAt().asc())
        .select(SOURCE)
        .execute();
  }

  default SocialLoginTransactionEntity findTransactionForUpdate(String stateHash) {
    return sql()
        .createQuery(TX)
        .where(TX.stateHash().eq(stateHash))
        .select(TX)
        .forUpdate()
        .fetchOneOrNull();
  }

  default SocialIdentityBindingEntity findBinding(UUID tenant, UUID sourceId, String subject) {
    return sql()
        .createQuery(BINDING)
        .where(
            BINDING.tenantId().eq(tenant),
            BINDING.sourceId().eq(sourceId),
            BINDING.subject().eq(subject))
        .select(BINDING)
        .fetchOneOrNull();
  }

  default void touchBinding(UUID id) {
    sql()
        .createUpdate(BINDING)
        .set(BINDING.lastLoginAt(), Instant.now())
        .where(BINDING.id().eq(id))
        .execute();
  }

  default void consumeTransaction(UUID id) {
    sql()
        .createUpdate(TX)
        .set(TX.consumedAt(), Instant.now())
        .where(TX.id().eq(id), TX.consumedAt().isNull())
        .execute();
  }
}
