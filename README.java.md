# Easy1Auth Java backend

Java 21 / Spring Boot 3.5 backend. The repository root is the Gradle build root.

```bash
docker compose -f docker-compose.java-dev.yml up -d postgres
export JAVA_HOME=$(/usr/libexec/java_home -v 21) # macOS when another JDK is the default
export ADMIN_JWT_SECRET="$(openssl rand -base64 48)"
./gradlew test
./gradlew :apps:admin-api:bootRun
```

`admin-api` listens on `18848`; `authorization-server` listens on `18850`. Applications validate Flyway state and never run DDL. Run migrations explicitly with `./gradlew :database-migration:bootRun` or enable the dedicated migration container.

Production Compose, immutable-image, bootstrap, maintenance, backup, cutover, and rollback
procedures are documented in [`docs/java-migration/phase-7-runbook.md`](docs/java-migration/phase-7-runbook.md).

## Admin authentication (phase 3)

`ADMIN_JWT_SECRET` is mandatory and must contain at least 32 UTF-8 bytes. Access tokens use the configured issuer, the fixed admin audience, a 15-minute default lifetime, subject type `admin`, and the account security version. Refresh tokens are opaque, stored only as SHA-256 digests, rotated on every use, and revoked after password/status/MFA changes.

Self-registration consumes a one-time, email-bound record from `admin_registration_code`. No development code is returned by an API and no default registration code exists. Until the phase-6 mail adapter issues these records, operators/tests must provision them through a controlled bootstrap process.
