# AGENTS.md — Easy1Auth

企业级多用户池身份与访问管理平台（SaaS），管理端界面使用中文。

## Monorepo 目录结构

```text
apps/ 和 modules/  Java 21 + Spring Boot 3.5 后端（根 Gradle 构建）
database-migration/ Flyway 数据库迁移与初始化应用
frontend/           Vue 3 + Vite + shadcn-vue + Tailwind CSS 4 + Reka UI
docs/               文档
```

Java 后端以仓库根目录作为 Gradle 构建根目录。`frontend/` 是独立的 pnpm 包，
不属于根目录 pnpm workspace。

## 当前技术栈

- PostgreSQL 是唯一的应用数据库。
- 前端依赖使用 pnpm 管理。
- UI 使用 shadcn-vue、Reka UI 和 Tailwind CSS 4。
- 管理 API 端口：`18848`；授权服务器端口：`18850`；前端端口：`18849`。

## 开发命令

Java 命令在仓库根目录运行，前端命令在 `frontend/` 目录运行：

```bash
# Java 后端
docker compose -f docker-compose.java-dev.yml up -d postgres
./gradlew :database-migration:bootRun
./gradlew :apps:admin-api:bootRun             # 端口 18848
./gradlew :apps:authorization-server:bootRun  # 端口 18850

# 前端
cd frontend && pnpm dev    # Vite 开发服务器，端口 18849
cd frontend && pnpm build  # vue-tsc --build && vite build
```

Vite 将 `/api` 代理到 `http://localhost:18848`，配置见 `frontend/vite.config.ts`。

`docker-compose.java-dev.yml` 提供开发环境 PostgreSQL；生产环境编排配置为
`deploy/compose.production.yml`。

## 关键架构决策

### 认证与多用户池

- JWT Token 存储在 `localStorage` 中，不使用 httpOnly Cookie。
- 所有需要认证的请求必须携带 `Authorization: Bearer <token>` 请求头。
- 通过 `user-pool-id` 请求头实现用户池隔离；该请求头由
  `frontend/apps/admin-console/src/utils/request.ts` 中的 Axios 拦截器设置。
- 管理 API 通过 `UserPoolContextFilter` 解析用户池上下文，并在领域服务中执行权限校验。

### 前端约定

- [antdv-next组件库文档](https://antdv-next.com/llms.txt)
- 在处理前端antdv-next相关问题时必需调用`antdv-next`skill，禁止猜测
- `@/*` 别名指向 `frontend/apps/admin-console/src/`，同时配置于 `tsconfig.json` 和 `vite.config.ts`。
  基础色为 `neutral`。
- 自定义共享组件放在 `frontend/apps/admin-console/src/components/common/`。
- Pinia Store 使用组合式 API 风格，目前统一放在 `user.ts`。

### 前端编码规范

- 不得将error等消息放在页面中，应当使用toast或message显示

### 路由

前端路由配置位于 `frontend/apps/admin-console/src/router/index.ts`：

- 所有需要认证的页面都是 `/` 的子路由，并使用 `MainLayout`。
- 登录页位于 `/login`，不使用主布局，是独立页面。
- OAuth2 授权页位于 `/oauth2/authorize`，不使用主布局。
- 路由守卫检查 `localStorage.getItem('token')`；无 Token 时重定向到 `/login`。

### 后端约定

- 在使用`jimmer`时需要调用`context7`来查询文档，禁止使用已有记忆进行猜测
- 禁止使用单行写法

## 当前缺失项

- 前端没有测试：不存在 Vitest/Jest 配置或前端测试运行器；Java 测试位于各 Gradle 子项目中。
- 前端没有代码检查：不存在 ESLint/Prettier 配置。
- 没有 CI/CD

## 文件命名约定

- 前端 API 模块：`frontend/apps/admin-console/src/api/*.ts`，每个资源一个文件并导出对象字面量。
- 前端视图：`frontend/apps/admin-console/src/views/<resource>/index.vue`。
- Vue 单文件组件：使用 `<script setup lang="ts">` 和组合式 API。

## 概念

- 用户池（`UserPool`）是原有“租户”的正式名称，继续作为后台切换、套餐、配额、资源隔离和授权登录的边界。
- 租户（`Tenant`）属于一个用户池，一个用户池可拥有多个租户。目前只维护基础资料，使用方式待定；不得将业务租户接入后台切换、管理员成员关系、权限、配额、Token 或授权登录上下文。
- 历史物理表 `tenant` / `tenant_membership` / `tenant_package` 仍表示用户池；V27 已将各表的用户池外键统一改为 `user_pool_id`，管理员最近选择字段为 `last_user_pool_id`。新租户存储在 `pool_tenant`，通过 `user_pool_id` 关联用户池。
- 管理端请求头使用 `user-pool-id`，历史 `tenant-id` 作为用户池 ID 的兼容别名。授权 issuer `/t/{id}` 和旧 Token 的 `tenant_id` 同样表示用户池，不能解释为新租户 ID。

- `pool_user`是用来给第三方介入的用户体系，给第三方授权登录。不应在系统后台登录，token也不得混用。
- 用户池：一个管理员`admin_user`拥有多个用户池，可切换。用户池绑定用户池套餐，用户池套餐有权限和套餐内容(用户数等)
- 角色：不要混淆`pool_user`的角色和`admin_user`的角色。两者不是同一个角色管理，两者对应的权限列表也是不同的。
- 品牌管理：登录样式指的是`pool_user`对应的登录页的样式，而不是`admin_user`登录页的样式。

## 部署

- 控制台域名: `console.easy1auth.shhsu.com`
- 授权服务域名: `auth.easy1auth.shhsu.com`
