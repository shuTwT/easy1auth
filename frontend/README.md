# Easy1Auth 前端 workspace

`frontend/` 是独立的 pnpm workspace，包含两个可以分别构建和部署的 Vue 应用：

- `apps/admin-console`：管理端，默认开发端口 `18849`，调用 `admin-api` 的 `/api`。
- `apps/auth-portal`：pool_user 登录和 OAuth 授权交互页，默认开发端口 `18851`，同源 API 由授权服务提供。

常用命令：

```bash
pnpm install
pnpm dev                 # 保持原有习惯，启动管理端
pnpm dev:auth            # 启动授权门户
pnpm typecheck
pnpm build               # 构建两个应用
pnpm build:admin
pnpm build:auth
```

授权门户开发服务器只代理 `/auth-portal-api`、`/oauth-consent/start` 和 `/t` 到 `http://localhost:18850`，不会调用管理 API。
