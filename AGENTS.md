# Easy1Auth — Agent Guide

## Project Overview

企业级多租户身份认证与访问管理(IAM) SaaS 平台. Monorepo with separate `frontend/` and `backend/` packages.

---

## Quick Start (actual commands, not docs)

```bash
# Backend (port 18848, not 3000 as README says)
cd backend && npm run dev          # tsx watch src/index.ts

# Frontend (port 18849, not 5173 as README says)
cd frontend && npm run dev          # vite

# Prisma
cd backend && npm run prisma:migrate    # prisma migrate dev
cd backend && npm run prisma:generate   # prisma generate
cd backend && npm run prisma:studio     # prisma studio
```

Frontend proxies `/api` → `http://localhost:18848` via Vite config.

---

## Actual Tech Stack vs Documentation

| Aspect | Documented | Actual |
|---|---|---|
| Database | PostgreSQL | SQLite via `@prisma/adapter-libsql` (Turso) |
| Backend port | 3000 | **18848** |
| Frontend port | 5173 | **18849** |
| Dev runner | ts-node (nodemon.json, stale) | `tsx watch` |
| Linting | ESLint + Prettier | **Not configured** |
| Testing | Mentioned in README | **No test files or framework** |
| CI/CD | GitHub Actions | **Not configured** |

**Do NOT trust README/PRD for ports, database, or linting — verify against source.**

---

## Database

- **SQLite** via Prisma + `@prisma/adapter-libsql`. File: `backend/dev.db`.
- docker-compose.yml includes a PostgreSQL service but it is **not used** by the running backend.
- Prisma v7.4.2. Datasource declared without explicit URL in schema — URL comes from `DATABASE_URL` env var (default: `file:./dev.db`).
- 7 migration directories in `backend/prisma/migrations/`.
- Array fields stored as JSON (SQLite limitation). Prisma schema uses `Json` type for arrays.

---

## Backend Architecture

**Entry**: `backend/src/index.ts` → Express 5 app.

**Pattern**: Mixed — some modules use Service classes (`tenant.service.ts`, `oauth2.service.ts`, `token.service.ts`), others inline Prisma in routes (`auth.routes.ts`, `tenant.routes.ts`). Both are acceptable; follow the pattern of the file you're modifying.

**Middleware chain** (order in index.ts):
1. `helmet()` → `cors()` → `morgan('combined')` → `express.json()`
2. Route groups (`/api/auth`, `/api/tenants`, etc.)
3. `auditMiddleware()` — captures request/response for logging
4. `errorHandler()` — catches `AppError` and unknown errors

**Key middleware files**:
- `middleware/auth.ts` — JWT verification, sets `req.userId`, reads `tenant-id` header
- `middleware/tenant.ts` — Tenant lookup & validation, `tenantDataFilter`, `checkTenantLimits`
- `middleware/errorHandler.ts` — `AppError` class with `statusCode` + `status` field
- `middleware/audit.middleware.ts` — Fire-and-forget audit logging via `audit.service.ts`

**API Route groups** (all under `/api`):
- `auth/` — login, register, send-code, passkey, social, refresh, logout
- `tenants/` — CRUD + status + domain check
- `users/` — CRUD + password reset + role/group assignment
- `applications/` — CRUD + client credentials
- `oauth2/` — authorize, token, userinfo, jwks, discover
- `audit-logs/` — query, stats, export, cleanup
- `groups/` — CRUD + hierarchy + members
- `positions/` — CRUD
- `roles/` — CRUD + hierarchy + user assignment
- `social-identity-providers/` — CRUD
- `brand-settings/` — brand settings CRUD
- `security/` — password policy, MFA, CORS, security settings
- `custom-domains/` — domain CRUD + verify
- `message-templates/` — CRUD
- `login-style/` — login page customization

**Auth tokens**: JWT via `jsonwebtoken`. `JWT_SECRET` from env or hardcoded fallback. Admin tokens expire in 7d, refresh tokens 30d. OAuth2 tokens stored in Redis via `token.service.ts`.

**Tenant isolation**: Every model has `tenantId` field. Middleware reads `tenant-id` header from frontend requests. Frontend sends it via Axios interceptor.

**Error responses** always use format: `{ status: 'error'|'success', message: string }` or `{ status, message, data }` for success.

---

## Frontend Architecture

**Entry**: `frontend/src/main.ts` — Vue 3 app with Pinia, Element Plus, Vue Router.

**Path alias**: `@/` → `src/` (configured in both vite.config.ts and tsconfig.app.json).

**Directory structure**:
- `api/` — Axios-based API modules, one per backend resource
- `types/` — TypeScript interfaces, roughly mirror backend types
- `stores/` — Pinia stores (currently only `user.ts`)
- `composables/` — Vue composables (currently `useAuth.ts`)
- `views/` — Page components, one subdirectory per feature
- `layouts/` — `MainLayout.vue` with sidebar navigation
- `components/` — Shared components
- `utils/` — `request.ts` (Axios instance with interceptors)
- `router/` — `index.ts` with lazy-loaded routes + auth guard

**Routing pattern**: All authenticated routes nested under `MainLayout`, lazy-loaded. Login and OAuth2 authorize are standalone routes. Auth guard checks `localStorage.getItem('token')`.

**API request pattern**: Axios instance with interceptors:
- Request: adds `Authorization: Bearer <token>` and `tenant-id` header
- Response: unwraps `response.data` (so callers get the body directly)
- Error: handles 401 → redirect to /login, displays ElMessage for other codes

**State management**: Pinia composition store (`useUserStore`) with localStorage persistence for token, currentTenant, currentTenantId.

**UI component library**: Element Plus, icons from `@element-plus/icons-vue` (globally registered in main.ts).

---

## Key Conventions

1. **Chinese error messages** throughout backend and frontend — maintain this pattern.
2. **Error response format** is always `{ status: 'error'|'success', message: string }`.
3. **No ESLint/Prettier** — don't add formatting tooling unless asked.
4. **No tests exist** — don't assume testing infrastructure.
5. **`<script setup lang="ts">`** for all Vue SFCs.
6. **Composition API** with `ref`/`reactive`/`computed` — no Options API.
7. **Lazy-loaded routes** with `() => import('@/views/...')`.
8. **Element Plus form validation rules** in component scope.
9. **Tenant header** is mandatory for multi-tenant API calls.
10. **Error handling**: Backend wraps errors in `AppError` class; frontend shows `ElMessage.error()` with `error.response?.data?.message`.

---

## Notable Gotchas

- `nodemon.json` is stale — the `dev` script actually uses `tsx watch`, not `ts-node`.
- README.md and PRD.md contain inaccurate port numbers, database provider, and tooling claims. Do not use them as authoritative sources.
- The PostgreSQL container in docker-compose.yml is unused by the running app (SQLite is used instead).
- `prisma.config.ts` exists at the backend root (generated by Prisma) but the schema declares datasource inline without URL — `DATABASE_URL` env var is the source of truth.
- `verification-code.service.ts` and `verificationCode.service.ts` both exist (duplicate, different casing). Be consistent when touching either.
- Backend needs `DATABASE_URL` env var — defaults to SQLite file `./dev.db`.
- Frontend build command is `vue-tsc -b && vite build` — requires TypeScript type-checking to pass first.
- Frontend dev server uses port 18849 with Vite proxy to backend at 18848.
- Backend port defaults to 18848 in `index.ts`: `process.env.PORT || 18848`.
