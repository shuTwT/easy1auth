package com.easy1auth.audit.repository;

import com.easy1auth.audit.dto.Query;
import com.easy1auth.audit.model.AuditEventEntity;
import com.easy1auth.audit.model.AuditEventEntityTable;
import com.easy1auth.infrastructure.foundation.web.PageData;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

public interface AuditEventRepository extends JRepository<AuditEventEntity, UUID> {
  AuditEventEntityTable EVENT = AuditEventEntityTable.$;

  default void saveEvent(AuditEventEntity event) {
    sql().saveCommand(event).setMode(SaveMode.INSERT_ONLY).execute();
  }

  default long deleteBefore(UUID tenant, Instant cutoff) {
    return sql()
        .createDelete(EVENT)
        .where(EVENT.tenantId().eq(tenant), EVENT.createdAt().lt(cutoff))
        .execute();
  }

  default PageData<AuditEventEntity> page(UUID tenant, int page, int size, Query queryInput) {
    var query =
        sql()
            .createQuery(EVENT)
            .where(EVENT.tenantId().eq(tenant))
            .whereIf(
                queryInput != null
                    && queryInput.actorName() != null
                    && !queryInput.actorName().isBlank(),
                () -> EVENT.actorName().ilike(queryInput.actorName(), LikeMode.ANYWHERE))
            .whereIf(
                queryInput != null
                    && queryInput.eventType() != null
                    && !queryInput.eventType().isBlank(),
                () -> EVENT.eventType().eq(queryInput.eventType()))
            .whereIf(
                queryInput != null && queryInput.action() != null && !queryInput.action().isBlank(),
                () -> EVENT.action().eq(queryInput.action()))
            .whereIf(
                queryInput != null
                    && queryInput.outcome() != null
                    && !queryInput.outcome().isBlank(),
                () -> EVENT.outcome().eq(queryInput.outcome()))
            .whereIf(
                queryInput != null && queryInput.start() != null,
                () -> EVENT.createdAt().ge(queryInput.start()))
            .whereIf(
                queryInput != null && queryInput.end() != null,
                () -> EVENT.createdAt().le(queryInput.end()))
            .orderBy(EVENT.createdAt().desc())
            .select(EVENT);
    return PageData.of(
        query.limit(size, (long) (page - 1) * size).execute(),
        page,
        size,
        query.fetchUnlimitedCount());
  }

  default Optional<AuditEventEntity> find(UUID tenant, UUID id) {
    return sql()
        .createQuery(EVENT)
        .where(EVENT.tenantId().eq(tenant), EVENT.id().eq(id))
        .select(EVENT)
        .fetchOptional();
  }

  default List<Tuple2<String, Instant>> statsRows(UUID tenant) {
    return sql()
        .createQuery(EVENT)
        .where(EVENT.tenantId().eq(tenant))
        .select(EVENT.outcome(), EVENT.createdAt())
        .execute();
  }
}
