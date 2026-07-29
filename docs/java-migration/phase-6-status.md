# Phase 6 implementation status

## Status

Phase 6 is implemented in the Java codebase. The Java and Vue builds pass. PostgreSQL-backed coverage is included, but tests that require Testcontainers remain skipped on this workstation when Docker/OrbStack is unavailable and must pass before phase-7 acceptance.

## Security

- tenant PoolUser policies and a separate platform administrator baseline are implemented;
- passwords are checked against complexity and history policy on password changes;
- administrator and PoolUser login failures use PostgreSQL counters and timed lockout;
- TOTP enrollment, AES-GCM encrypted secrets, replay prevention, one-time challenges and hashed recovery codes are implemented;
- administrator JWTs and PoolUser authorization sessions are issued only after required MFA;
- email verification challenges are hashed, rate-limited and delivered through the PostgreSQL email outbox;
- PoolUser device observations are tenant qualified and do not bypass MFA;
- SECURITY_DATA_ENCRYPTION_SECRET is required and has no default.

## Federation and customization

- federation is intentionally limited to generic OIDC providers;
- provider secrets are encrypted and returned only on creation;
- Authorization Code, PKCE S256, state, nonce, Discovery issuer checks, ID Token signature/audience checks, explicit bindings and optional JIT provisioning are implemented;
- issuer, provider, login transaction, binding and PoolUser must belong to the same tenant;
- branding, constrained login styles, message templates and custom-domain inventory use Jimmer;
- arbitrary custom CSS is stored only for compatibility and is never rendered on authentication pages;
- domain ownership verification and certificate/private-key upload are intentionally unavailable; TLS remains a gateway responsibility.

## Audit and delivery

- append-style tenant audit events redact credential-shaped fields;
- management mutations are captured with actor, path, result, IP, user agent and trace context;
- audit list, detail, statistics and retention cleanup contracts are available;
- email and Webhook delivery use a PostgreSQL outbox with leases, retry backoff and dead-letter state;
- Webhook secrets are one-time values, encrypted at rest, and requests use timestamped HMAC-SHA256 signatures;
- Webhook creation and delivery reject loopback, link-local and private-network destinations.

## Frontend compatibility

- administrator login handles mfa_required without storing a token before verification;
- the social identity management page now exposes only generic OIDC configuration;
- arbitrary login-page CSS execution was removed;
- custom-domain verification and certificate upload controls are disabled and explain the phase-6 boundary.

## Verification

- unit tests cover RFC 6238 windows/replay and encryption AAD isolation;
- the PostgreSQL suite covers phase-6 migration, tenant policy isolation, encrypted OIDC secrets, TOTP state, custom-domain isolation and audit redaction;
- existing phase 3–5 tests remain part of the full Gradle suite;
- run clean test assemble with Java 21 and all three required secrets;
- run pnpm build in frontend.

## Phase 7 prerequisites

- execute all Testcontainers suites on a Docker-enabled runner;
- test SMTP and Webhook delivery against controlled staging endpoints;
- test generic OIDC against at least one production-like provider;
- configure managed SECURITY_DATA_ENCRYPTION_SECRET, SMTP credentials, canonical issuer origin and gateway TLS;
- keep custom domains out of issuer calculation.
