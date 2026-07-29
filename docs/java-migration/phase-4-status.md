# Phase 4 implementation status

## Status

Phase 4 implementation is complete. The Java backend now covers the existing Vue contracts for users, groups, positions, roles and permissions. PostgreSQL-backed acceptance tests are present and run automatically when Docker is available; the local verification on 2026-07-29 skipped those tests because the Docker/OrbStack daemon was unavailable.

## Implemented

- Flyway schemas for `pool_user`, `user_group`, `position`, `pool_role`, `pool_permission` and their user/group/admin/role association tables;
- tenant-local unique constraints for usernames, emails, group names, position codes, role codes and permission codes;
- `tenant_id` plus tenant-qualified composite foreign keys on all phase-4 association tables;
- tenant-qualified parent and department foreign keys, preventing cross-tenant hierarchy references at the database boundary;
- Jimmer entities and Jimmer-only business persistence; repositories do not use Spring `JdbcClient`;
- user CRUD, status, statistics, password reset/change, role assignment and group assignment;
- user creation quota checks under a tenant row lock in the same transaction as insertion;
- XLSX import template, 5 MB upload limit, row parsing and per-row error reporting;
- group CRUD, tree, statistics, parent-cycle checks, member/admin listing and assignment;
- position CRUD and statistics;
- role CRUD, tree, statistics, user assignment, immutable system-role rules and effective data-scope precedence;
- permission CRUD, tree, statistics, cycle checks and concurrency-safe idempotent preset initialization;
- tenant-qualified reads, updates and deletes for resources and association rows;
- Vue-compatible response shapes for `/api/users`, `/api/groups`, `/api/positions`, `/api/roles` and `/api/permissions`.

## Verification

- `./gradlew clean test assemble --no-daemon`: passes;
- frontend `pnpm build`: passes;
- architecture tests: pass;
- phase-4 controller contract tests: pass;
- Excel template/import tests: pass;
- PostgreSQL Testcontainers suite covers known-ID cross-tenant access, cross-tenant association attempts, hierarchy cycles, system-role protection, JSONB role permissions, data-scope precedence, preset idempotency and concurrent quota enforcement;
- on the current machine, all PostgreSQL Testcontainers cases are reported as skipped only because no Docker daemon is available.

## Follow-up outside phase 4

- Run the Testcontainers suite in CI or a workstation with Docker before deployment acceptance.
- `pool_user.position` remains the legacy string field, so position `userCount` matches users by tenant and position name. A future normalized position membership should use a position ID.
- Phase 3's full TOTP/MFA flow and full JWT HTTP integration suite remain separate unfinished phase-3 work; they do not change the phase-4 directory and user-access implementation status.
