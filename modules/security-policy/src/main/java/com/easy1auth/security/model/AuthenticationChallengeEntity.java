package com.easy1auth.security.model;
import org.babyfish.jimmer.sql.*; import org.jspecify.annotations.Nullable; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="authentication_challenge") public interface AuthenticationChallengeEntity {
 @Id UUID id(); @Column(name="token_hash") String tokenHash(); @Column(name="subject_type") String subjectType();
 @Nullable @Column(name="subject_id") UUID subjectId(); @Nullable @Column(name="tenant_id") UUID tenantId(); String purpose();
 @Column(name="factor_type") String factorType(); @Nullable @Column(name="code_hash") String codeHash(); int attempts();
 @Column(name="max_attempts") int maxAttempts(); @Column(name="expires_at") Instant expiresAt(); @Nullable @Column(name="consumed_at") Instant consumedAt();
 @Column(name="created_at") Instant createdAt(); @Nullable @Column(name="last_sent_at") Instant lastSentAt();
}
