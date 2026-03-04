# Easy1Auth - 企业级统一身份管理平台

<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![License](https://img.shields.io/badge/license-ISC-green.svg)
![Node](https://img.shields.io/badge/node-%3E%3D18.0.0-brightgreen.svg)
![Vue](https://img.shields.io/badge/vue-3.x-brightgreen.svg)

**企业级多租户身份认证与访问管理(SaaS)解决方案**

[功能特性](#功能特性) • [快速开始](#快速开始) • [技术栈](#技术栈) • [项目结构](#项目结构) • [开发指南](#开发指南)

</div>

---

## 📖 项目简介

Easy1Auth 是一个功能完整的企业级统一身份管理平台,提供集中化的用户身份管理、单点登录(SSO)、权限控制和安全审计能力。支持多租户架构,适合作为企业内部身份管理系统或SaaS身份认证服务。

### 核心能力

- 🔐 **统一身份认证** - 支持OAuth 2.0、OIDC、SAML等多种协议
- 🏢 **多租户架构** - 完善的租户隔离与数据安全
- 👥 **用户生命周期管理** - 用户注册、认证、授权、审计全流程
- 🔑 **单点登录(SSO)** - 一次登录,处处通行
- 🛡️ **细粒度权限控制** - RBAC权限模型,支持数据权限
- 🔒 **企业级安全** - MFA多因素认证、密码策略、设备管理
- 🎨 **高度可定制** - 自定义登录页、域名、消息模板等
- 📊 **完整审计日志** - 所有操作可追溯,满足合规要求

---

## ✨ 功能特性

### 核心功能模块

| 模块 | 功能描述 |
|------|---------|
| **租户管理** | 多租户注册、配置、计费管理 |
| **用户管理** | 用户注册、信息管理、状态管理 |
| **应用管理** | 应用注册、配置、密钥管理 |
| **单点登录** | OAuth 2.0、OIDC、SAML 2.0、CAS |
| **权限管理** | 角色定义、权限分配、数据权限 |
| **用户组管理** | 组织架构、用户组层级管理 |
| **岗位管理** | 岗位定义、岗位权限、编制管理 |
| **社会化身份源** | 微信、钉钉、飞书、GitHub等 |
| **安全设置** | CORS、密码策略、MFA、设备管理 |
| **个性化设置** | 登录框样式、域名、消息模板 |
| **Webhook** | 事件订阅、实时通知 |
| **审计日志** | 操作日志、安全审计、日志分析 |

---

## 🚀 快速开始

### 环境要求

- Node.js >= 18.0.0
- PostgreSQL >= 13.0
- Redis >= 6.0
- npm 或 yarn

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/yourusername/easy1auth.git
cd easy1auth
```

#### 2. 安装依赖

```bash
# 安装前端依赖
cd frontend
npm install

# 安装后端依赖
cd ../backend
npm install
```

#### 3. 配置数据库

创建PostgreSQL数据库:

```sql
CREATE DATABASE easy1auth;
```

配置环境变量:

```bash
# 后端配置
cd backend
cp .env.example .env
# 编辑 .env 文件,配置数据库连接信息
```

执行数据库迁移:

```bash
npm run prisma:migrate
```

#### 4. 启动服务

```bash
# 启动后端服务
cd backend
npm run dev

# 新终端窗口启动前端服务
cd frontend
npm run dev
```

访问应用:
- 前端: http://localhost:5173
- 后端API: http://localhost:3000
- API文档: http://localhost:3000/api-docs

---

## 🛠️ 技术栈

### 前端技术

- **框架**: Vue 3 (Composition API + TypeScript)
- **UI组件库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **构建工具**: Vite
- **代码规范**: ESLint + Prettier

### 后端技术

- **运行时**: Node.js 18+
- **框架**: Express.js
- **ORM**: Prisma
- **数据库**: PostgreSQL
- **缓存**: Redis
- **认证**: JWT (jsonwebtoken)
- **语言**: TypeScript

### 基础设施

- **容器化**: Docker + Docker Compose
- **CI/CD**: GitHub Actions
- **监控**: Prometheus + Grafana
- **日志**: ELK Stack / Loki

---

## 📁 项目结构

```
easy1auth/
├── frontend/                 # 前端项目
│   ├── src/
│   │   ├── assets/          # 静态资源
│   │   ├── components/      # 公共组件
│   │   ├── layouts/         # 布局组件
│   │   ├── router/          # 路由配置
│   │   ├── stores/          # 状态管理
│   │   ├── utils/           # 工具函数
│   │   ├── views/           # 页面组件
│   │   ├── App.vue          # 根组件
│   │   └── main.ts          # 入口文件
│   ├── public/              # 公共资源
│   ├── .env                 # 环境变量
│   ├── vite.config.ts       # Vite配置
│   └── package.json
│
├── backend/                  # 后端项目
│   ├── src/
│   │   ├── middleware/      # 中间件
│   │   ├── routes/          # 路由
│   │   ├── lib/             # 工具库
│   │   └── index.ts         # 入口文件
│   ├── prisma/
│   │   └── schema.prisma    # 数据库模型
│   ├── .env                 # 环境变量
│   ├── tsconfig.json        # TypeScript配置
│   └── package.json
│
├── PRD.md                    # 产品需求文档
├── TODO.md                   # 开发进度跟踪
└── README.md                 # 项目说明文档
```

---

## 📝 开发指南

### 开发命令

#### 前端

```bash
# 开发模式
npm run dev

# 构建生产版本
npm run build

# 代码检查
npm run lint

# 类型检查
npm run type-check
```

#### 后端

```bash
# 开发模式
npm run dev

# 构建
npm run build

# 生产模式
npm start

# Prisma命令
npm run prisma:generate   # 生成Prisma客户端
npm run prisma:migrate    # 运行数据库迁移
npm run prisma:studio     # 打开Prisma Studio
```

### 代码规范

- 使用TypeScript编写代码
- 遵循ESLint和Prettier配置
- 提交代码前运行lint检查
- 编写单元测试覆盖核心功能

### Git提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建/工具链相关
```

---

## 🔒 安全最佳实践

- ✅ 所有密码使用bcrypt加密存储
- ✅ JWT Token + Refresh Token机制
- ✅ HTTPS传输加密
- ✅ SQL注入防护(Prisma ORM)
- ✅ XSS防护
- ✅ CSRF防护
- ✅ 请求频率限制
- ✅ 敏感数据加密存储

---

## 📊 性能优化

- 数据库索引优化
- Redis缓存策略
- 前端代码分割与懒加载
- 静态资源CDN加速
- API响应压缩

---

## 🗺️ 路线图

### v1.0.0 (当前版本)
- ✅ 项目基础架构
- ✅ 核心数据模型
- ✅ 基础API接口
- ✅ 前端基础页面

### v1.1.0 (计划中)
- 🔲 完整的租户管理功能
- 🔲 用户注册登录功能
- 🔲 应用管理与SSO
- 🔲 基础权限管理

### v1.2.0 (计划中)
- 🔲 社会化身份源集成
- 🔲 MFA多因素认证
- 🔲 审计日志完善

---

## 🤝 贡献指南

欢迎贡献代码、报告Bug或提出新功能建议!

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

---

## 📄 许可证

本项目采用 ISC 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

---

## 📞 联系方式

- 项目主页: https://github.com/yourusername/easy1auth
- 问题反馈: https://github.com/yourusername/easy1auth/issues
- 邮箱: support@easy1auth.com

---

<div align="center">

**⭐ 如果这个项目对你有帮助,请给一个Star支持一下! ⭐**

Made with ❤️ by Easy1Auth Team

</div>
