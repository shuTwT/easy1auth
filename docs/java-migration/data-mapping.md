# Data mapping and differences

| Product concept | Prisma/SQLite today | PostgreSQL target | Migration rule |
|---|---|---|---|
| Global administrator | `Admin` mixes credentials and current tenant | `admin_account` plus separate credentials/session records | new data only |
| Tenant choice | `Admin.currentTenantId` | `admin_account.last_tenant_id` | preference, never authorization |
| Membership | `AdminTenant.role` | `tenant_membership`, unique account/tenant | role belongs to membership |
| Tenant owner | unconstrained role string | partial unique index for one active owner | transfer under tenant row lock |
| Pool user | `User` | `pool_user` | tenant-local username/email uniques |
| JSON settings | SQLite `Json` | PostgreSQL `jsonb` | normalize queryable relations |
| IDs and time | UUIDv4-like strings, mixed ISO time | UUIDv7 and `timestamptz` UTC | no historical conversion |

Only `database-migration` owns Flyway DDL. Both applications share the schema and set Flyway disabled; startup validates required schema objects.
