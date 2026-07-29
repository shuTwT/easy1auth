# Phase 5 implementation status

## Status

Phase 5 implementation is complete in the Java codebase. PostgreSQL protocol tests are implemented but are skipped on the current workstation because Docker/OrbStack is unavailable; they must run successfully in a Docker-enabled environment before deployment acceptance.

## Application and client management

- `/api/applications` list, detail, create, update, delete, status, statistics and secret-rotation contracts are implemented in `admin-api`;
- application persistence uses Jimmer and every lookup/mutation is tenant-qualified;
- application quota checks lock the tenant row and run in the same transaction as creation;
- confidential client secrets are generated with a CSPRNG, returned only on creation/rotation (or a public-to-confidential type transition), and stored only as BCrypt digests;
- SPA/native applications are public clients without secrets and must use PKCE;
- redirect and post-logout URIs are stored as exact URI lists; wildcard redirects and URI fragments are rejected;
- allowed grant types are restricted to Authorization Code, Refresh Token and Client Credentials;
- client type, token lifetimes, PKCE and consent settings are validated at the application boundary;
- deleting an application removes its authorization and consent protocol records.

## Authorization Server

- Spring Authorization Server 1.5.8 is configured with OIDC enabled;
- tenant issuer format is `https://{platform-host}/t/{tenantId}` (the scheme/origin is supplied by `OAUTH2_ISSUER_BASE`);
- discovery, authorization, token, revocation, JWK and UserInfo endpoints are tenant-prefixed;
- Authorization Code + S256 PKCE, Refresh Token rotation, Client Credentials, OIDC ID Token, UserInfo, revocation and RP-initiated logout are enabled;
- clients are resolved through a tenant-aware `RegisteredClientRepository` backed by Jimmer application data;
- authorization and consent state use Spring's JDBC protocol services with PostgreSQL storage and database-populated `tenant_id`;
- token and authorization lookups validate the current issuer tenant before returning protocol state;
- a pool user can authenticate only within the issuer tenant; a principal authenticated for another tenant is rejected before authorization;
- access and ID tokens contain the tenant, subject type, client audience and pool-user claims, including roles and groups;
- Refresh Tokens rotate (`reuseRefreshTokens=false`), and authorization codes retain Spring Authorization Server's single-use semantics;
- legacy Express tokens and administrator JWT signing keys are not accepted by this server.

## Signing keys and issuer safety

- each tenant receives an independently generated RSA-2048/RS256 signing key;
- public JWK metadata is stored in PostgreSQL;
- private JWK material is encrypted with AES-256-GCM using `OAUTH2_KEY_ENCRYPTION_SECRET` before persistence;
- no built-in signing or encryption secret exists; startup fails when the encryption secret is shorter than 32 UTF-8 bytes;
- `OAUTH2_ISSUER_BASE` restricts the accepted scheme, host and port, preventing Host-header issuer confusion;
- forwarded headers are processed for canonical gateway deployments; custom domains are not accepted as alternate issuers.

## Verification

- unit tests cover tenant issuer parsing, tenant-confused client lookup, client PKCE/rotation settings, canonical issuer-origin validation and pool-user tenant binding;
- admin controller contract tests cover the Vue application-list response envelope;
- PostgreSQL Testcontainers tests cover one-time secret storage, cross-tenant application lookup, discovery, JWK persistence, Client Credentials, JWT issuer/audience/tenant claims, exact redirect matching, mandatory PKCE, state, nonce, authorization-code single use, ID Token, UserInfo, Refresh Token rotation, revocation and tenant-confusion rejection;
- the application detail page now displays tenant-prefixed protocol endpoints and supports one-time secrets without expecting stored plaintext;
- frontend `pnpm build` passes;
- Java `clean test assemble` passes across all modules.

## Deployment requirements

- set `OAUTH2_KEY_ENCRYPTION_SECRET` to a managed secret of at least 32 UTF-8 bytes;
- set `OAUTH2_ISSUER_BASE` to the canonical externally visible platform origin, for example `https://auth.example.com`;
- route `/t/{tenantId}/**` to `authorization-server` while preserving trusted forwarded scheme/host information;
- execute Flyway V6 before starting either Java application;
- run the Docker-backed protocol suite before phase-7 cutover approval.
