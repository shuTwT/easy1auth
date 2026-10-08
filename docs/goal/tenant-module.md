# 用户池下的租户

一个用户池下可以维护多个租户，每个租户只能归属一个用户池。

当前租户只是一套基础资料，实际使用方式待定。不参与后台登录、后台切换、管理员角色、用户池用户分配、套餐、配额、数据隔离或 OAuth 授权。没有租户或停用、删除租户时，用户池内现有管理功能与第三方登录保持可用。

## 当前能力

- 查询当前用户池下的租户，按名称和状态筛选、分页。
- 创建、编辑和删除租户资料。
- 字段：名称、编码、描述、状态、创建时间和更新时间。
- 名称最多200个字符，编码为1至100位小写字母、数字、下划线或连字符；编码在同一用户池内唯一，不同用户池可以重复。
- 状态为启用（`active`）或停用（`inactive`），目前仅表示资料状态。
- 不预建默认租户，不强制将已有用户、应用、角色或管理员归属某个租户。

## 边界和入口

- 页面：`/pool-tenants`，菜单“租户管理”；页面所属范围由顶栏的用户池切换决定。
- API：`/api/pool-tenants`，所属用户池来自 `user-pool-id` 请求头；不允许请求体指定或转移所属用户池。
- 存储：`pool_tenant.user_pool_id` 指向历史 `tenant.id`（该历史表实际表示用户池）。
- 新权限：`menu:pool-tenant`、`pool-tenant:list/read/create/update/delete`，作用域为 `USER_POOL`；不形成租户级后台权限。

## 兼容说明

原租户管理已改名为用户池管理，正式页面为 `/user-pool`、套餐页面为 `/user-pool-package`；历史 `/tenant` 和 `/tenant-package` 页面重定向到对应用户池页面。

正式用户池管理 API 为 `/api/user-pools`，套餐 API 为 `/api/platform/user-pool-packages`；历史 `/api/tenants` 和 `/api/platform/tenant-packages` 路径保留为用户池接口别名。

现有数据库表、外键、稳定权限码（`tenant:*`、`tenant-package:*`）、角色值（`tenant_admin`）、授权 issuer（`/t/{用户池ID}`）和旧 Token claim（`tenant_id`）保持兼容，语义均为用户池。新签发的授权 Token 同时增加 `user_pool_id`。Java 和前端模型使用 `UserPool`、`userPoolId`、`userPoolPackage`，管理端登录响应使用 `userPools` 和 `currentUserPoolId`。

历史 Gradle 模块目录和 Java 包中的 `tenant` 暂时保留；其中 `UserPool*` 类表示用户池，新的 `TenantEntity` / `TenantService` 表示下属租户。

升级迁移 `V26` 新建租户表并调整菜单名称，不搬迁或重建已有用户池数据。套餐和管理员角色按原用户管理动作权限逐项复制租户资料权限，只读授权不会获得新增、编辑或删除权限。
