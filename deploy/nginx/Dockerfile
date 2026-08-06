# syntax=docker/dockerfile:1.7
FROM node:22.17.1-alpine3.22 AS frontend-build

WORKDIR /workspace/frontend
RUN corepack enable
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
COPY frontend/apps/ ./apps/
COPY frontend/packages/ ./packages/
RUN pnpm install --frozen-lockfile
RUN pnpm build:admin && pnpm build:auth

FROM nginx:1.28.0-alpine3.21
RUN rm -f /etc/nginx/conf.d/default.conf
COPY --from=frontend-build /workspace/frontend/apps/admin-console/dist/ /usr/share/nginx/html/admin/
COPY --from=frontend-build /workspace/frontend/apps/auth-portal/dist/ /usr/share/nginx/html/auth/
COPY deploy/nginx/easy1auth.conf /etc/nginx/conf.d/easy1auth.conf
COPY deploy/nginx/entrypoint.sh /usr/local/bin/easy1auth-nginx-entrypoint
RUN chmod 0555 /usr/local/bin/easy1auth-nginx-entrypoint \
    && printf '%s\n' '<!doctype html><html lang="zh-CN"><meta charset="utf-8"><title>系统维护</title><body><main><h1>系统维护中</h1><p>服务正在进行受控切换，请稍后重试。</p></main></body></html>' > /usr/share/nginx/html/maintenance.html

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/easy1auth-nginx-entrypoint"]
