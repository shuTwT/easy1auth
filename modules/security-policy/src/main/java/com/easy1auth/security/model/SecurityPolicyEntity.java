package com.easy1auth.security.model;
import org.babyfish.jimmer.sql.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="security_policy") public interface SecurityPolicyEntity {
 @Id @Column(name="tenant_id") UUID tenantId();
 @Column(name="password_min_length") int passwordMinLength(); @Column(name="password_require_upper") boolean passwordRequireUpper();
 @Column(name="password_require_lower") boolean passwordRequireLower(); @Column(name="password_require_number") boolean passwordRequireNumber();
 @Column(name="password_require_special") boolean passwordRequireSpecial(); @Column(name="password_max_age_days") int passwordMaxAgeDays();
 @Column(name="password_history_count") int passwordHistoryCount(); @Column(name="mfa_required") boolean mfaRequired();
 @Column(name="login_attempt_limit") int loginAttemptLimit(); @Column(name="lockout_duration_seconds") int lockoutDurationSeconds();
 @Column(name="updated_at") Instant updatedAt();
}
