package com.easy1auth.social.repository;

import com.easy1auth.social.model.SocialIdentitySourceEntity;
import com.easy1auth.social.model.*;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 社会化身份源及登录事务数据仓储。 */
public interface SocialIdentityRepository extends JRepository<SocialIdentitySourceEntity, UUID> {
    default SocialIdentitySourceEntity createSource(UUID id, UUID tenant, String name, String type, String mode, String clientId, String encryptedSecret, boolean jit) {
        Instant now = Instant.now();
        var entity = SocialIdentitySourceEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant).setName(name).setType(type).setMode(mode).setClientId(clientId).setEncryptedClientSecret(encryptedSecret).setJitProvisioning(jit).setStatus("active").setCreatedAt(now).setUpdatedAt(now));
        sql().saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
        return entity;
    }
    default SocialLoginTransactionEntity createTransaction(UUID id, UUID tenant, UUID source, String stateHash, String nonceHash, String encryptedNonce, String encryptedPkce, String returnUri, Instant expires) {
        var entity = SocialLoginTransactionEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant).setSourceId(source).setStateHash(stateHash).setNonceHash(nonceHash).setEncryptedNonce(encryptedNonce).setEncryptedPkceVerifier(encryptedPkce).setReturnUri(returnUri).setExpiresAt(expires).setConsumedAt(null).setCreatedAt(Instant.now()));
        sql().saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
        return entity;
    }
    default void createBinding(UUID tenant, UUID source, UUID user, String sourceType, String subject, Map<String, Object> claims) {
        var entity = SocialIdentityBindingEntityDraft.$.produce(d -> d.setId(UUID.randomUUID()).setTenantId(tenant).setSourceId(source).setPoolUserId(user).setSourceType(sourceType).setSubject(subject).setClaims(claims).setCreatedAt(Instant.now()).setLastLoginAt(Instant.now()));
        sql().saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
    }
}
