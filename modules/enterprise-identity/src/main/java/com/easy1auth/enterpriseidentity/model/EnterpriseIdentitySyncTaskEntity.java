package com.easy1auth.enterpriseidentity.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "enterprise_identity_sync_task")
public interface EnterpriseIdentitySyncTaskEntity extends BaseEntity, BaseTenantEntity {
    @Column(name = "source_id") java.util.UUID sourceId();
    String type();
    @Nullable @Column(name = "event_id") String eventId();
    @Serialized Map<String, Object> payload();
    String status();
    @Serialized Map<String, Object> summary();
    @Nullable @Column(name = "last_error") String lastError();
    @Column(name = "created_at") Instant createdAt();
    @Nullable @Column(name = "started_at") Instant startedAt();
    @Nullable @Column(name = "finished_at") Instant finishedAt();
}
