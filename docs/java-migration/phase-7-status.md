# Phase 7 implementation status

## Status

Deployment and cutover tooling is implemented. Production cutover approval is blocked because
all automated and manual acceptance testing remains explicitly deferred.

## Delivered

- fixed-version Java 21 and Vue/Nginx image definitions with digest-pinned production references;
- PostgreSQL 17, one-shot migration, explicit bootstrap profile, two non-root Java services, and
  an Nginx-only public gateway;
- Compose secrets exposed to Spring through config tree and fail-fast production validation;
- Flyway V7 startup enforcement, `/livez`, `/readyz`, gateway `/healthz`, and graceful shutdown;
- OAuth auxiliary paths moved away from Vue `/login` to `/oauth-login` and `/oauth-consent`;
- maintenance marker, immutable-image checks, migration/bootstrap/probe/backup operator commands;
- documented archive, cutover, rollback, and master-secret revocation procedures.

## Not performed

No Gradle task, Testcontainers suite, frontend build, browser test, manual business acceptance,
Compose cutover, or production mutation was executed in this phase implementation turn.
