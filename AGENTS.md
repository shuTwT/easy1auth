# AGENTS.md — Easy1Auth

企业级多租户身份与访问管理平台（SaaS），管理端界面使用中文。

## Monorepo 目录结构

```text
apps/ 和 modules/  Java 21 + Spring Boot 3.5 后端（根 Gradle 构建）
database-migration/ Flyway 数据库迁移与初始化应用
frontend/           Vue 3 + Vite + shadcn-vue + Tailwind CSS 4 + Reka UI
design-system/      设计规范（easy1auth-admin/MASTER.md）
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

### 认证与多租户

- JWT Token 存储在 `localStorage` 中，不使用 httpOnly Cookie。
- 所有需要认证的请求必须携带 `Authorization: Bearer <token>` 请求头。
- 通过 `tenant-id` 请求头实现租户隔离；该请求头由
  `frontend/src/utils/request.ts` 中的 Axios 拦截器设置。
- 管理 API 通过 `TenantContextFilter` 解析租户上下文，并在领域服务中执行权限校验。

### 前端约定

- [antdv-next组件库文档](https://antdv-next.com/llms.txt)
- `@/*` 别名指向 `frontend/src/`，同时配置于 `tsconfig.json` 和 `vite.config.ts`。
- shadcn-vue 配置文件为 `frontend/components.json`，样式为 `reka-nova`，
  基础色为 `neutral`。
- UI 组件位于 `frontend/src/components/ui/`，由 shadcn-vue CLI 管理。
- 自定义共享组件放在 `frontend/src/components/common/`。
- Pinia Store 使用组合式 API 风格，目前统一放在 `user.ts`。
- Toast 通知使用 `vue-sonner`，不使用 Element Plus Message。
- 表单使用 `vee-validate` 和 `zod`，不使用 Element Plus 表单校验。
- 表格使用 `@tanstack/vue-table`，不使用 Element Plus Table。

### 前端编码规范

- 不得将error等消息放在页面中，应当使用toast或message显示

### 双配色方案

目前存在两套可能不一致的颜色配置：

1. `frontend/src/styles/theme.css`：主色为蓝色 `#0369A1`，侧边栏使用深色渐变。
   这是应用实际加载的主题，由 `main.ts` 引入。
2. `design-system/easy1auth-admin/MASTER.md`：主色为紫色 `#7C3AED`，
   辅色为橙色 `#F97316`。这是设计规范，可能尚未完整应用。

修改颜色时必须同时检查这两个来源，并明确哪一个是当前需求的事实来源。

### 路由

前端路由配置位于 `frontend/src/router/index.ts`：

- 所有需要认证的页面都是 `/` 的子路由，并使用 `MainLayout`。
- 登录页位于 `/login`，不使用主布局，是独立页面。
- OAuth2 授权页位于 `/oauth2/authorize`，不使用主布局。
- 路由守卫检查 `localStorage.getItem('token')`；无 Token 时重定向到 `/login`。

## 当前缺失项

- 前端没有测试：不存在 Vitest/Jest 配置或前端测试运行器；Java 测试位于各 Gradle 子项目中。
- 前端没有代码检查：不存在 ESLint/Prettier 配置。
- 没有 CI/CD

## 设计系统文件

构建或修改 UI 时，按以下顺序检查：

1. `design-system/easy1auth-admin/MASTER.md`：全局设计规则、颜色、字体和组件规范。
2. `design-system/easy1auth-admin/pages/`：页面级设计覆盖，优先级高于 `MASTER.md`。
3. `frontend/src/styles/theme.css`：应用当前实际加载的 CSS 变量。
4. `frontend/components.json`：shadcn-vue 配置。

## 文件命名约定

- 前端 API 模块：`frontend/src/api/*.ts`，每个资源一个文件并导出对象字面量。
- 前端视图：`frontend/src/views/<resource>/index.vue`。
- Vue 单文件组件：使用 `<script setup lang="ts">` 和组合式 API。

## 概念

- `pool_user`是用来给第三方介入的用户体系，给第三方授权登录。不应在系统后台登录，token也不得混用。
- 租户：一个管理员`admin_user`拥有多个租户，可切换。租户绑定租户套餐，租户套餐有权限和套餐内容(用户数等)
- 角色：不要混淆`pool_user`的角色和`admin_user`的角色。两者不是同一个角色管理，两者对应的权限列表也是不同的。
- 品牌管理：登录样式指的是`pool_user`对应的登录页的样式，而不是`admin_user`登录页的样式。