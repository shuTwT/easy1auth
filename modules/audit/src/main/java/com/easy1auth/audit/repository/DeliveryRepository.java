package com.easy1auth.audit.repository;

import com.easy1auth.audit.model.WebhookSubscriptionEntity;
import com.easy1auth.audit.model.WebhookSubscriptionEntityTable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

public interface DeliveryRepository extends JRepository<WebhookSubscriptionEntity, UUID> {
  WebhookSubscriptionEntityTable HOOK = WebhookSubscriptionEntityTable.$;

  default void saveSubscription(WebhookSubscriptionEntity subscription) {
    sql().saveCommand(subscription).setMode(SaveMode.INSERT_ONLY).execute();
  }

  default List<WebhookSubscriptionEntity> listSubscriptions() {
    return sql().createQuery(HOOK).orderBy(HOOK.createdAt().desc()).select(HOOK).execute();
  }

  default int updateSubscription(
      UUID tenant,
      UUID id,
      String name,
      String url,
      List<String> events,
      int retries,
      String status) {
    return sql()
        .createUpdate(HOOK)
        .set(HOOK.name(), name)
        .set(HOOK.url(), url)
        .set(HOOK.events(), events)
        .set(HOOK.maxRetries(), retries)
        .set(HOOK.status(), status)
        .set(HOOK.updatedAt(), Instant.now())
        .where(HOOK.tenantId().eq(tenant), HOOK.id().eq(id))
        .execute();
  }

  default int rotateSubscriptionSecret(
      UUID tenant, UUID id, String secretHash, String encryptedSecret) {
    return sql()
        .createUpdate(HOOK)
        .set(HOOK.secretHash(), secretHash)
        .set(HOOK.encryptedSecret(), encryptedSecret)
        .set(HOOK.updatedAt(), Instant.now())
        .where(HOOK.id().eq(id), HOOK.tenantId().eq(tenant))
        .execute();
  }

  default int deleteSubscription(UUID tenant, UUID id) {
    return sql().createDelete(HOOK).where(HOOK.id().eq(id), HOOK.tenantId().eq(tenant)).execute();
  }

  default List<WebhookSubscriptionEntity> findActiveSubscriptions(UUID tenant) {
    return sql()
        .createQuery(HOOK)
        .where(HOOK.tenantId().eq(tenant), HOOK.status().eq("active"))
        .select(HOOK)
        .execute();
  }

  default Optional<WebhookSubscriptionEntity> findSubscription(UUID tenant, UUID id) {
    return sql()
        .createQuery(HOOK)
        .where(HOOK.tenantId().eq(tenant), HOOK.id().eq(id))
        .select(HOOK)
        .fetchOptional();
  }

  default Optional<WebhookSubscriptionEntity> findSubscription(UUID id) {
    return sql().createQuery(HOOK).where(HOOK.id().eq(id)).select(HOOK).fetchOptional();
  }
}
