# Phase 3 implementation status

Implemented:

- password login with BCrypt cost 12 and generic invalid-credential responses;
- short-lived HS256 admin JWT with mandatory external key, issuer, admin audience, subject type, and security version;
- database-backed opaque refresh sessions with SHA-256 digest storage, rotation, revocation, expiry, and replay rejection;
- one-time email-bound registration-code consumption (delivery adapter remains phase 6);
- real-time active-account/security-version validation for every access token;
- real-time account, tenant, and membership validation for tenant-scoped administrator endpoints;
- membership-scoped roles and permission catalog, owner wildcard, and privilege-escalation prevention;
- admin-user read/update/status/password/MFA-reset/role/remove endpoints with self-disable, self-remove, and owner protections;
- owner transfer remains serialized by the tenant row lock from phase 2.
- SMTP-backed registration-code delivery with opt-in development code exposure;
- registration atomically creates the first tenant, owner membership, and default system roles;
- bodyless frontend logout invalidates the account security version and all refresh sessions;
- self-service password change and cross-tenant owner disable protection;
- exact admin-user/admin-role statistics and the complete legacy permission catalog.

Deferred to the security-policy work in phase 6: TOTP enrollment/challenge, backup-code lifecycle, email delivery, lockout counters, and password-history policy. No placeholder MFA secret or verification bypass is provided.

Verification on 2026-07-29: `clean test assemble` passed. PostgreSQL Testcontainers execution was not possible because the local Docker/OrbStack daemon was not running.
