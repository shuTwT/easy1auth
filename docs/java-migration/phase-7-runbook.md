# Phase 7 deployment and cutover runbook

## Status and hard boundary

This phase prepares deployable images, PostgreSQL migration/bootstrap tasks, Nginx routing,
health probes, maintenance mode, and operator procedures. It does **not** approve a production
cutover. Tests are intentionally deferred and none of the commands in this runbook may be used
to open production traffic until the acceptance matrix at the end of this document passes.

The former Express/SQLite implementation has been removed from this repository. There is no
importer, dual write, or automatic PostgreSQL-to-SQLite replay.

## Artifacts and immutable images

Production uses [compose.production.yml](../../deploy/compose.production.yml). It contains only:

- PostgreSQL 17 on an internal network and a persistent volume;
- the one-shot `database-migration` task;
- the explicit-profile `bootstrap-admin` task;
- non-root Java 21 `admin-api` and `authorization-server` containers;
- an Nginx gateway serving the Vue production build.

It contains no Redis or Express service. Java containers use a read-only root filesystem,
bounded temporary storage, dropped capabilities, resource limits, a 20-second Spring shutdown
phase, and a 30-second Compose stop grace period.

Build a release without executing tests:

```bash
deploy/bin/easy1auth-ops build-images 0.1.0-phase7
```

Push each versioned image to the approved registry, then record registry digests:

```bash
deploy/bin/easy1auth-ops lock-digests \
  registry.example.com/easy1auth/database-migration:0.1.0-phase7 \
  registry.example.com/easy1auth/admin-api:0.1.0-phase7 \
  registry.example.com/easy1auth/authorization-server:0.1.0-phase7 \
  registry.example.com/easy1auth/gateway:0.1.0-phase7
```

Copy `deploy/.env.production.example` to the ignored `deploy/.env.production`, merge the four
generated `repository@sha256:digest` values, and set the canonical origins, SMTP endpoint, and
permitted sender address. SMTP defaults to port 465 with implicit TLS and certificate hostname
validation.
The operator wrapper refuses application images that are not digest-pinned. Base images in the
Dockerfiles and PostgreSQL image are fixed versions; `latest` is not used.

Record the following in the release ticket before a cutover: Git commit, all image digests,
the SHA-256 of `deploy/.env.production` after redacting values from the ticket, the Nginx config
version, and expected Flyway version `7`.

## Secrets and configuration tree

Create the ignored files listed in [the secret inventory](../../deploy/secrets/README.md) with
mode `0600`. Compose mounts them under `/run/secrets` using Spring config-tree property names.
PostgreSQL uses `POSTGRES_PASSWORD_FILE`. Bootstrap credentials are mounted only into the
explicit bootstrap task.

The `production` profile rejects missing, short, or common placeholder values for the database
password and the three master secrets. The admin API also requires SMTP credentials. Keep
custom domains out of `OAUTH2_ISSUER_BASE`; it must be one canonical HTTPS origin without a path.

## Migration and first administrator

Start PostgreSQL, apply migrations, inspect state, and validate checksums:

```bash
docker compose --env-file deploy/.env.production -f deploy/compose.production.yml up -d postgres
deploy/bin/easy1auth-ops migrate
deploy/bin/easy1auth-ops flyway-info
deploy/bin/easy1auth-ops flyway-validate
```

Both applications refuse startup unless `flyway_schema_history` contains a successful V7 row.
The bootstrap command also migrates first, requires the `bootstrap-admin` Spring profile, reads
credentials only from secret files, and refuses to run when any administrator exists:

```bash
deploy/bin/easy1auth-ops bootstrap-admin
```

It creates only `admin_account` and `admin_credential` with UUIDv7 identifiers and BCrypt cost
12. It creates no tenant, role, default password, PoolUser, or OAuth client, and logs only the
new account ID. After first login, create the first tenant through `POST /api/tenants`; the
existing tenant service makes that administrator the owner.

## Maintenance mode and probes

Start the stack with external business routes blocked:

```bash
deploy/bin/easy1auth-ops up-maintenance
deploy/bin/easy1auth-ops readiness
```

The gateway serves static Vue assets and `/healthz` while returning `503` for `/api/**`,
`/t/**`, `/oauth-login/**`, and `/oauth-consent/**`. Operators can reach application probes on
the internal Compose network. Toggle the marker without rebuilding or restarting:

```bash
deploy/bin/easy1auth-ops maintenance-on
deploy/bin/easy1auth-ops maintenance-off
```

Each Java application exposes `/livez` (Spring context/process only) and `/readyz` (database,
successful Flyway V7, and required configuration). Authorization readiness validates the issuer
origin but deliberately does not call the signing-key service and therefore does not generate
keys for tenants. Nginx exposes `/healthz` for its own process.

Nginx routes `/api/**` to `admin-api:18848`; `/t/**`, `/oauth-login/**`, and
`/oauth-consent/**` to `authorization-server:18850`; all other paths use Vue history fallback.
The Vue `/login` route is untouched. This production config assumes TLS terminates at Nginx or
its immediately adjacent trusted certificate control plane, so it supplies the fixed external
protocol `X-Forwarded-Proto: https`, plus normalized host and direct-peer client-address headers.
Do not expose port 8080 around that TLS boundary or forward arbitrary client-supplied headers.

After creating the first tenant, check its canonical issuer, Discovery document, and JWK:

```bash
deploy/bin/easy1auth-ops oidc TENANT_UUID https://auth.example.com
```

The Discovery `issuer` must equal `https://auth.example.com/t/TENANT_UUID`. A custom-domain
host must not change that value.

## Backup and restore

Create and hash a PostgreSQL backup:

```bash
mkdir -p deploy/backups
deploy/bin/easy1auth-ops backup-postgres deploy/backups/post-cutover.dump
```

Restoration is destructive and requires explicit operator approval and a maintenance window:

```bash
ALLOW_RESTORE=yes deploy/bin/easy1auth-ops restore-postgres deploy/backups/approved.dump
```

If a pre-migration Express/SQLite archive is retained for compliance, keep it outside this
repository in the approved read-only artifact and secret archives. Never restore it into this
working tree or print its configuration into a release ticket.

## Master-secret revocation and rotation

All three procedures require maintenance mode, a fresh PostgreSQL backup, two-person approval,
and a recorded incident/change ticket. Never try a secret replacement while either Java service
is writing.

1. **Administrator JWT secret:** stop `admin-api`; delete all `admin_refresh_session` rows and
   increment every `admin_account.security_version`; replace `admin_jwt_secret.txt`; restart and
   require all administrators to authenticate again. Existing administrator JWTs and refresh
   tokens are invalid.
2. **OAuth signing-key encryption secret:** stop both Java services; delete OAuth authorizations,
   consents, and all `oauth2_signing_key` rows; replace `oauth2_key_encryption_secret.txt`;
   restart in maintenance mode and deliberately visit JWK for each approved tenant to generate
   new keys. All existing OIDC tokens, codes, refresh tokens, and sessions must be treated as
   revoked. Do not delete keys without accepting that impact.
3. **Security data encryption secret:** there is no transparent re-wrap command. Safe rotation is
   destructive revocation: remove authentication factors/challenges and transient federation
   login transactions, reset administrator MFA state, and disable federation providers and
   Webhook subscriptions whose stored secrets use the old key. Replace
   `security_data_encryption_secret.txt`, restart in maintenance mode, then require MFA
   re-enrollment and manual provider/Webhook secret replacement. Do not retain encrypted rows
   and assume the new key can read them.

Use reviewed SQL prepared for the specific incident; this repository intentionally does not ship
a one-click destructive rotation script.

## Cutover sequence (only after the acceptance gate passes)

Maintain a 15-minute observation window before ordinary users can write.

1. Freeze the release and record image digests, configuration version, and Flyway V7.
2. Prevent issuance of tokens from any superseded deployment.
3. Start a new PostgreSQL volume, migrate, validate, and run the one-time bootstrap.
4. Start both Java applications and Nginx in maintenance mode.
5. Check `/livez`, `/readyz`, V7, the first tenant issuer, Discovery, and JWK.
6. Route `/api/**`, `/t/**`, and the auxiliary OAuth pages to the new gateway.
7. Complete the 15-minute maintenance observation and then remove the maintenance marker.
8. Clear legacy frontend tokens and require administrators and PoolUsers to sign in again.

## Rollback

Before new writes are opened: enable maintenance, stop the candidate Java services, restore the
last approved PostgreSQL backup if required, route traffic back to the previous immutable Java
release, and invalidate tokens produced during the rehearsal.

After new writes are opened: immediately re-enable maintenance and stop Java writes; back up
PostgreSQL and inventory all new records requiring manual disposition. Roll back to the previous
immutable Java release only with explicit human approval, using a database version compatible
with that release. Invalidate administrator JWTs, OIDC tokens, refresh tokens, and sessions
created during the cutover.

## Production acceptance gate (currently blocked)

The deployment remains **deployment-ready, not cutover-approved** until all of the following are
restored and pass with zero relevant skips/failures:

- Java `clean test assemble` and all PostgreSQL Testcontainers suites;
- Vue production build;
- administrator login, MFA, first-tenant creation, and tenant switching;
- user/application management and the cross-tenant attack matrix;
- OIDC Code + PKCE, Refresh, UserInfo, Revocation, Discovery, and JWK;
- SMTP, Webhook retry, and audit-event flows;
- Compose cold start, migration failure, restart, and graceful shutdown;
- maintenance mode, traffic cutover, and full rollback rehearsal.
