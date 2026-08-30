package com.easy1auth.audit.repository;

import com.easy1auth.audit.model.DeliveryOutboxEntity;
import com.easy1auth.audit.model.DeliveryOutboxEntityTable;
import com.easy1auth.audit.model.WebhookSubscriptionEntity;
import com.easy1auth.audit.model.WebhookSubscriptionEntityTable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

public interface DeliveryRepository extends JRepository<WebhookSubscriptionEntity, UUID> {
  WebhookSubscriptionEntityTable HOOK = WebhookSubscriptionEntityTable.$;
  DeliveryOutboxEntityTable OUT = DeliveryOutboxEntityTable.$;

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

  default List<DeliveryOutboxEntity> claim(int limit, Instant now) {
    var rows =
        sql()
            .createQuery(OUT)
            .where(
                Predicate.or(
                    OUT.status().eq("pending"),
                    Predicate.and(OUT.status().eq("processing"), OUT.leaseUntil().lt(now))),
                OUT.availableAt().le(now))
            .orderBy(OUT.availableAt())
            .select(OUT)
            .limit(Math.min(50, Math.max(1, limit)))
            .forUpdate()
            .execute();
    rows.forEach(row -> markProcessing(row.id(), now));
    return rows;
  }

  default int markSent(UUID id, Instant now) {
    return sql()
        .createUpdate(OUT)
        .set(OUT.status(), "sent")
        .set(OUT.sentAt(), now)
        .set(OUT.leaseUntil(), (Instant) null)
        .where(OUT.id().eq(id))
        .execute();
  }

  default DeliveryOutboxEntity findOutboxForUpdate(UUID id) {
    return sql().createQuery(OUT).where(OUT.id().eq(id)).select(OUT).forUpdate().fetchOne();
  }

  default int markFailed(UUID id, String status, int attempts, Instant availableAt, String error) {
    return sql()
        .createUpdate(OUT)
        .set(OUT.status(), status)
        .set(OUT.attempts(), attempts)
        .set(OUT.availableAt(), availableAt)
        .set(OUT.leaseUntil(), (Instant) null)
        .set(OUT.lastError(), error)
        .where(OUT.id().eq(id))
        .execute();
  }

  default void saveOutbox(DeliveryOutboxEntity outbox) {
    sql().saveCommand(outbox).setMode(SaveMode.INSERT_IF_ABSENT).execute();
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

  private void markProcessing(UUID id, Instant now) {
    sql()
        .createUpdate(OUT)
        .set(OUT.status(), "processing")
        .set(OUT.leaseUntil(), now.plusSeconds(60))
        .where(OUT.id().eq(id))
        .execute();
  }
}
