FROM node:22-alpine

WORKDIR /workspace/frontend
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/admin-console/package.json apps/admin-console/package.json
COPY apps/auth-portal/package.json apps/auth-portal/package.json
COPY packages/ packages/
RUN pnpm install --frozen-lockfile
COPY apps/ apps/

EXPOSE 18849 18851
CMD ["pnpm", "--filter", "@easy1auth/admin-console", "dev", "--host"]
