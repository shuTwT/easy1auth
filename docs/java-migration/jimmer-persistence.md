# Jimmer persistence boundary

Business repositories use Jimmer `JSqlClient`, generated table types, drafts, fetchers, and associations. Repository classes are prohibited from depending on Spring `JdbcClient`; an ArchUnit rule enforces this.

Current native SQL exceptions are deliberately narrow:

- `RegistrationCodeNativeSql`: PostgreSQL `DELETE … RETURNING` atomically consumes a one-time registration code.
- `SchemaValidation`: application-startup infrastructure checks that the separately managed Flyway schema exists.

Tenant authorization crosses module boundaries through `TenantAuthorizationProvider`. Its admin-access implementation uses Jimmer and prevents the tenant module from querying admin-access or admin-identity tables directly.
