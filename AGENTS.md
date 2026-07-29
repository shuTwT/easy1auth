# AGENTS.md — Easy1Auth

Enterprise multi-tenant IAM platform (SaaS). Chinese-language admin UI.

## Monorepo layout

```
apps/ and modules/  Java 21 + Spring Boot 3.5 backend (root Gradle build)
database-migration/ Flyway migration and bootstrap application
frontend/           Vue 3 + Vite + shadcn-vue + Tailwind CSS 4 + Reka-UI
design-system/  Design specs (easy1auth-admin/MASTER.md)
```

The Java backend is a Gradle build rooted at the repository root. `frontend/` is an independent
pnpm package and is not part of a root pnpm workspace.

## Current stack

- PostgreSQL is the only application database.
- Frontend packages use pnpm.
- The UI uses shadcn-vue + Reka-UI + Tailwind CSS 4.
- Admin API: `18848`; authorization server: `18850`; frontend: `18849`.

## Development commands

Java commands run from the repository root. Frontend commands run from `frontend/`:

```bash
# Java backend
docker compose -f docker-compose.java-dev.yml up -d postgres
./gradlew :database-migration:bootRun
./gradlew :apps:admin-api:bootRun       # port 18848
./gradlew :apps:authorization-server:bootRun # port 18850

# Frontend
cd frontend && pnpm dev         # Vite dev server (port 18849)
cd frontend && pnpm build       # vue-tsc --build && vite build
```

Vite proxies `/api` → `http://localhost:18848` (see `frontend/vite.config.ts`).

`docker-compose.java-dev.yml` provides the development PostgreSQL database. Production
orchestration is `deploy/compose.production.yml`.

## Key architecture decisions

### Auth & multi-tenancy

- **JWT tokens** stored in `localStorage` (not httpOnly cookies).
- All authenticated requests require `Authorization: Bearer <token>` header.
- Tenant isolation via `tenant-id` request header (set by axios interceptor in `frontend/src/utils/request.ts`).
- The admin API resolves tenant context in `TenantContextFilter` and enforces access in domain services.

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

- **Frontend has no tests** — No vitest/jest config or frontend test runner. Java tests exist under the Gradle subprojects.
- **Frontend has no linting** — No ESLint/Prettier config.
- **No CI/CD** — Mentioned in TODO.md but not implemented.

## Design system files

When building UI, check these files first:

1. `design-system/easy1auth-admin/MASTER.md` — Global design rules, colors, typography, component specs.
2. `design-system/easy1auth-admin/pages/` — Per-page design overrides (override MASTER.md).
3. `frontend/src/styles/theme.css` — Actual CSS variables currently loaded.
4. `frontend/components.json` — shadcn-vue configuration.

## File naming conventions

- Frontend API modules: `frontend/src/api/*.ts` (one per resource, exports object literal)
- Frontend views: `frontend/src/views/<resource>/index.vue`
- Vue SFC: `<script setup lang="ts">` with Composition API

## Sisyphus configuration

- `.sisyphus/` directory exists for work plans
- `.agents/` directory contains project-local frontend skills
- `skills-lock.json` tracks installed skill versions
