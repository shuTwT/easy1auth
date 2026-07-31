package com.easy1auth.security.model;
import com.easy1auth.persistence.model.BaseEntity; import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="pool_user_device") public interface PoolUserDeviceEntity extends BaseEntity, BaseTenantEntity {
 @Column(name="user_id") UUID userId(); @Column(name="device_token_hash") String deviceTokenHash();
 @Nullable @Column(name="user_agent") String userAgent(); @Nullable @Column(name="ip_address") String ipAddress(); @Column(name="first_seen_at") Instant firstSeenAt(); @Column(name="last_seen_at") Instant lastSeenAt(); @Nullable @Column(name="revoked_at") Instant revokedAt();
}
