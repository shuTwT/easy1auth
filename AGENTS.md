# AGENTS.md — Easy1Auth

Enterprise multi-tenant IAM platform (SaaS). Chinese-language admin UI.

## Monorepo layout

```
frontend/   Vue 3 + Vite + shadcn-vue + Tailwind CSS 4 + Reka-UI
backend/    Express + Prisma v7 + SQLite
design-system/  Design specs (easy1auth-admin/MASTER.md)
```

Each package is independent — separate `pnpm-workspace.yaml`, separate `node_modules`. **Not** a root workspace.

## ⚠️ README is out of date — trust the code, not the README

| Claim in README | Actual |
|---|---|
| PostgreSQL | **SQLite** (`provider = "sqlite"` in schema, `@prisma/adapter-libsql`) |
| npm | **pnpm** (only lockfiles are `pnpm-lock.yaml`) |
| Element Plus UI | **shadcn-vue + Reka-UI + Tailwind CSS 4** |
| Backend port 3000 | **18848** (`backend/src/index.ts`) |
| Frontend port 5173 | **18849** (`frontend/vite.config.ts`) |

SQLite is the dev default. `@prisma/adapter-pg` is installed as a dependency but unused. DB file: `backend/data/dev.db`.

## Development commands

All commands run from within `frontend/` or `backend/`:

```bash
# Backend
cd backend && pnpm dev          # tsx watch src/index.ts (port 18848)
cd backend && pnpm db:migrate   # prisma migrate dev
cd backend && pnpm db:seed      # seed data (default admin: admin/Admin123!@#)
cd backend && pnpm db:studio    # prisma studio

# Frontend
cd frontend && pnpm dev         # Vite dev server (port 18849)
cd frontend && pnpm build       # vue-tsc --build && vite build
```

Vite proxies `/api` → `http://localhost:18848` (see `frontend/vite.config.ts`).

### Running a single Prisma migration

```bash
cd backend && npx prisma migrate dev --name <name>
```

Docker Compose exists (`docker-compose.yml`) with PostgreSQL + Redis, but it is **not** the primary dev workflow.

## Key architecture decisions

### Backend: routes-call-services, sometimes routes-call-Prisma-directly

Route files live in `backend/src/routes/`. Most delegate to class-based services in `backend/src/services/` (e.g., `UserService`, `TenantService`), but **auth.routes.ts calls Prisma directly**. New code should follow the service pattern.

API response shape: `{ status: 'ok' | 'error', data?, message? }`

Error handling uses `AppError` class from `middleware/errorHandler.ts`:
```ts
throw new AppError('message', 400)  // 4xx → status: 'fail', 5xx → status: 'error'
```

### Prisma v7 with adapter pattern

```ts
// backend/src/lib/prisma.ts
const adapter = new PrismaLibSql({ url: 'file:./data/dev.db' })
const prisma = new PrismaClient({ adapter })
```

- **Always create PrismaClient with the adapter.** Direct `new PrismaClient()` will not work with the SQLite setup.
- Seed file (`prisma/seed.ts`) and any standalone scripts must replicate this pattern.
- Prisma config: `backend/prisma.config.ts`

### Auth & multi-tenancy

- **JWT tokens** stored in `localStorage` (not httpOnly cookies).
- All authenticated requests require `Authorization: Bearer <token>` header.
- Tenant isolation via `tenant-id` request header (set by axios interceptor in `frontend/src/utils/request.ts`).
- Middleware chain: auth → tenant → (optional) tenantDataFilter/checkTenantLimits.
- `tenantDataFilter` auto-injects `tenantId` into `req.body`.
- Auth middleware uses `AuthRequest` type extension; tenant middleware uses `TenantRequest`.

### Frontend: shadcn-vue conventions

- `@/*` alias → `frontend/src/`. Configured in both `tsconfig.json` + `vite.config.ts`.
- shadcn-vue config: `frontend/components.json` (style: "reka-nova", baseColor: "neutral").
- UI components are in `frontend/src/components/ui/` (36 components), managed by shadcn-vue CLI.
- Custom shared components go in `frontend/src/components/common/`.
- Pinia store: single `user.ts` store (Composition API style).
- Toast notifications: `vue-sonner` (not Element Plus's message).
- Forms: `vee-validate` + `zod` (not Element Plus form validation).
- Table: `@tanstack/vue-table` (not Element Plus table).

### Dual color schemes

Two color configs exist; they may diverge:

1. **`frontend/src/styles/theme.css`** — Blue primary `#0369A1`, dark sidebar gradient. This is what the app actually loads (imported in `main.ts`).
2. **`design-system/easy1auth-admin/MASTER.md`** — Purple `#7C3AED` + orange `#F97316`. Design spec, may or may not be fully applied.

When changing colors, check both sources and decide which is the source of truth.

### Routing

Frontend router (`frontend/src/router/index.ts`):
- All authenticated pages are children of `/` with `MainLayout`.
- Login page at `/login` (no layout, standalone page).
- OAuth2 authorize at `/oauth2/authorize` (no layout).
- Route guard checks `localStorage.getItem('token')` — no token → redirect to `/login`.

## What's missing

- **No tests** — No vitest/jest config, no test runner scripts, no `*.test.ts` or `*.spec.ts` files.
- **No linting** — No ESLint/Prettier config in either package.
- **No CI/CD** — Mentioned in TODO.md but not implemented.
- **No type-check script** — Backend lacks a `tsc --noEmit` or equivalent. Frontend runs `vue-tsc --build` as part of `pnpm build`.

## Design system files

When building UI, check these files first:

1. `design-system/easy1auth-admin/MASTER.md` — Global design rules, colors, typography, component specs.
2. `design-system/easy1auth-admin/pages/` — Per-page design overrides (override MASTER.md).
3. `frontend/src/styles/theme.css` — Actual CSS variables currently loaded.
4. `frontend/components.json` — shadcn-vue configuration.

## File naming conventions

- Backend routes: `*.routes.ts` (plural, lowercase kebab prefix)
- Backend services: `*.service.ts`
- Backend types: `*.types.ts`
- Frontend API modules: `frontend/src/api/*.ts` (one per resource, exports object literal)
- Frontend views: `frontend/src/views/<resource>/index.vue`
- Vue SFC: `<script setup lang="ts">` with Composition API

## Sisyphus configuration

- `.sisyphus/` directory exists for work plans
- `.agents/` directory contains project-local skills (Prisma agent skills)
- `skills-lock.json` tracks installed skill versions
