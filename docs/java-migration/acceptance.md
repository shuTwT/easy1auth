# Migration acceptance cases

- A tenant creation transaction creates both tenant and owner membership, or neither.
- A forged `tenant-id`, disabled membership, disabled account, or suspended tenant returns 403.
- Two concurrent owner transfers serialize on the tenant row; the partial index prevents a second owner.
- Owner deletion and owner self-removal fail until ownership is transferred.
- Cross-tenant read/update/delete by a known UUID returns no resource and makes no change.
- Applications start only against a Flyway-migrated PostgreSQL schema and never create tables.
- Testcontainers PostgreSQL is the integration-test database; H2 is not present.
- No default production account, password, JWT key, OAuth signing key, or client secret exists.
