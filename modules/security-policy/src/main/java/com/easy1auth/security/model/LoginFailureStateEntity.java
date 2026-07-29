package com.easy1auth.security.model;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="login_failure_state") public interface LoginFailureStateEntity {
 @Id UUID id(); @Column(name="subject_type") String subjectType(); @Column(name="subject_key") String subjectKey(); @Nullable @Column(name="tenant_id") UUID tenantId();
 @Column(name="failed_attempts") int failedAttempts(); @Nullable @Column(name="locked_until") Instant lockedUntil(); @Nullable @Column(name="last_failed_at") Instant lastFailedAt();
}
