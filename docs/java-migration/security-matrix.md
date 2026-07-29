# Security and permission matrix

| Action | Required relationship | Required privilege | Mandatory invariant |
|---|---|---|---|
| Select tenant | active account + active membership + active tenant | any membership | `tenant-id` is only input, never proof |
| Create tenant | authenticated admin account | none | creator becomes owner in same transaction |
| Transfer owner | active membership | current owner | tenant row lock; exactly one owner afterward |
| Remove member | active membership | owner or delegated permission | owner cannot be removed directly or self-remove |
| Read/update tenant resource | active membership | resource permission | query by `(tenant_id, resource_id)` |
| Grant admin role | active membership | role grant permission | cannot grant permissions actor lacks |

Known legacy defects that are intentionally incompatible: default JWT secrets, accepting `tenant-id` without membership validation, global tenant listing for ordinary admins, and update/delete by naked resource ID.
