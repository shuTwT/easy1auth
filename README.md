# Easy1Auth

Easy1Auth 是面向企业与 SaaS 场景的多租户身份认证与访问管理平台，提供管理员后台、
OAuth 2.0 / OpenID Connect 授权服务、用户与组织管理、RBAC、安全策略和审计能力。

## 技术栈

- 后端：Java 21、Spring Boot 3.5、Spring Security、Spring JDBC
- 数据库：PostgreSQL 17、Flyway
- 前端：Vue 3、Vite、TypeScript、shadcn-vue、Reka UI、Tailwind CSS 4
- 构建：Gradle Wrapper（后端）、pnpm（前端）

## 项目结构

```text
easy1auth/
├── apps/
│   ├── admin-api/              # 管理 API，端口 18848
│   └── authorization-server/   # OAuth2/OIDC 授权服务，端口 18850
├── modules/                    # 领域与基础设施模块
├── database-migration/         # Flyway 迁移和管理员初始化应用
├── build-logic/                # Gradle 约定插件
├── frontend/                   # Vue 管理前端，端口 18849
├── design-system/              # 设计规范
├── deploy/                     # 生产部署与运维配置
├── docker-compose.java-dev.yml # 本地 PostgreSQL
└── README.java.md              # 后端运行和部署说明
```

## 本地开发

要求：JDK 21、Docker、Node.js 和 pnpm。

```bash
# 1. 启动 PostgreSQL
docker compose -f docker-compose.java-dev.yml up -d postgres

# 2. 设置管理员 JWT 密钥并执行数据库迁移
export ADMIN_JWT_SECRET="$(openssl rand -base64 48)"
./gradlew :database-migration:bootRun

# 3. 启动后端应用（分别在两个终端运行）
./gradlew :apps:admin-api:bootRun
./gradlew :apps:authorization-server:bootRun

# 4. 启动前端
cd frontend
pnpm install
pnpm dev
```

访问地址：

- 管理前端：<http://localhost:18849>
- 管理 API：<http://localhost:18848>
- 授权服务：<http://localhost:18850>

Vite 将 `/api` 请求代理到管理 API。

## 验证

```bash
# Java 测试
./gradlew test

# 前端类型检查和生产构建
cd frontend
pnpm build
```

生产部署、初始化、备份和维护流程见
[`docs/java-migration/phase-7-runbook.md`](docs/java-migration/phase-7-runbook.md)。
