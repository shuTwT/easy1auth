# API compatibility inventory

The admin boundary retains `{ status, data?, message? }`; several legacy endpoints use top-level `tenant`/`tenants`, which must remain until the Vue client is migrated. Pagination commonly uses `page` and `pageSize`.

| Prefix | Current capability | Java owner | Phase |
|---|---|---|---:|
| `/api/auth` | admin/user login, registration, code, passkey, social, refresh, logout | admin-identity | 3 |
| `/api/tenants` | list, create, current, update, status, delete | tenant | 2–3 |
| `/api/admin-users`, `/api/admin-roles` | membership administration and membership-scoped RBAC | admin-access | 3 |
| `/api/users`, `/api/groups`, `/api/positions` | pool directory CRUD and assignments | directory | 4 |
| `/api/roles`, `/api/permissions` | pool-user RBAC and data scope | user-access | 4 |
| `/api/applications` | OAuth client management and secret rotation | application | 5 |
| `/api/oauth2` | legacy OAuth endpoints (replaced at cutover) | oauth2-core | 5 |
| `/api/security` | password and MFA policy | security-policy | 6 |
| `/api/social-identity-providers` | provider CRUD and callback | federation | 6 |
| `/api/brand-settings`, `/api/custom-domains`, `/api/login-style`, `/api/message-templates` | tenant customization | customization | 6 |
| `/api/audit-logs`, `/api/dashboard` | audit query and projections | audit | 6 |

New protocol endpoints are tenant qualified: `/t/{tenantId}/.well-known/openid-configuration`, `/oauth2/authorize`, `/oauth2/token`, `/oauth2/revoke`, `/oauth2/jwks`, and `/userinfo` beneath that tenant prefix.
