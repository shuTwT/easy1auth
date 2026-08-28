package com.easy1auth.audit.repository;
import com.easy1auth.audit.model.AuditEventEntity;
import com.easy1auth.audit.model.AuditEventEntityTable;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import java.time.Instant; import java.util.UUID;
public interface AuditEventRepository extends JRepository<AuditEventEntity, UUID> {
    AuditEventEntityTable EVENT = AuditEventEntityTable.$;
    default void saveEvent(AuditEventEntity event) { sql().saveCommand(event).setMode(SaveMode.INSERT_ONLY).execute(); }
    default long deleteBefore(UUID tenant, Instant cutoff) { return sql().createDelete(EVENT).where(EVENT.tenantId().eq(tenant), EVENT.createdAt().lt(cutoff)).execute(); }
}
