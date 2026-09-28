# 身份源分类与命名

| 分类 | 功能 | 前端路由 | 页面、API 和类型模块 | 支持的厂商类型 |
| --- | --- | --- | --- | --- |
| 同步身份源 | 导入及同步用户和用户组，查看同步任务与配置事件回调 | `/sync-identity-source` | `syncIdentitySource` | `feishu` |
| 企业身份源 | 企业应用登录授权 | `/enterprise-identity-source` | `enterpriseIdentitySource` | `feishu_web` |
| 社会化身份源 | 社交账号登录授权 | `/social-identity-source` | `socialIdentitySource` | `wechat_qr`、`wechat_mp`、`github`、`gitee` |

企业身份源与社会化身份源共用 `LoginIdentitySourceManager` 表单组件，通过页面明确传入的
`category` 区分，创建选项、列表筛选和统计均按分类隔离。共用的登录模型为
`loginIdentitySource`，其中 `SocialSourceType` 仅包含社会化厂商，`EnterpriseSourceType`
仅包含企业登录厂商。同步模型独立，包含通讯录应用凭证和同步任务。

企业和社会化登录沿用 `/api/social-identity-sources` 后端协议与 `social_identity_source`
存储，企业查询和统计明确指定 `type=feishu_web`。前端请求携带明确的 `category`，
管理接口校验记录所属分类，前端同时检查返回记录和提交类型，
防止在错误分类中读取或修改身份源。统计由数据库按类型和状态聚合，不受列表分页限制。

同步管理沿用历史地址 `/api/enterprise-identity-sources`、数据库
`enterprise_identity_source` 及已有管理权限码。历史地址仅由前端 `syncIdentitySourceApi`
使用；已配置的飞书事件回调地址和已有同步数据保持有效。同步回调的公开域名可通过
`VITE_SYNC_IDENTITY_PUBLIC_BASE_URL` 设置，兼容历史变量
`VITE_ENTERPRISE_IDENTITY_PUBLIC_BASE_URL`。

登录页样式中的“第三方登录”可以选择已启用的企业和社会化登录身份源，并在选项中标注
分类。发布协议继续使用 `socialProviderIds` 字段，不包含同步身份源。
