# 应用管理

创建并维护应用用来接入easy1auth

## prisma字段
```
  id                   String   @id @default(uuid())
  tenantId             String
  name                 String
  logo                 String?
  description          String?
  type                 String   @default("web")
  clientId             String   @unique
  clientSecret         String
  redirectUris         Json
  allowedGrantTypes    Json
  accessTokenLifetime  Int      @default(3600)
  refreshTokenLifetime Int      @default(2592000)
  status               String   @default("active")
  createdAt            DateTime @default(now())
  updatedAt            DateTime @updatedAt
```

## 前端表单

- 应用名称
- 应用类型
- 应用描述
- 应用logo
- 重定向URI
- 授权类型
- 访问令牌有效期
- 刷新令牌有效期

## 前端搜索表单

- 应用名称
- 应用类型
- 状态

## 前端列表

- 应用名称
- 应用类型
- Client ID
- 状态
- 访问令牌有效期
- 刷新令牌有效期
- 创建时间
