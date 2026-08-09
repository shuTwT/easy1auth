-- V11: 社会化身份源重构
-- 将通用 OIDC 联邦（federation 模块）重写为预定义社交登录厂商（social-identity 模块）。
-- 表重命名 + 新增 type/mode 列 + 权限码重命名 social-identity-provider -> social-identity-source。
-- 按计划「完全移除」通用 OIDC，清空三张 federation 表的存量数据后重建为 social_identity_*。

-- 1. 重命名三张表（保留结构，清空数据后逐步调整列）
alter table external_identity_binding rename to social_identity_binding;
alter table federation_provider rename to social_identity_source;
alter table federation_login_transaction rename to social_login_transaction;

-- 2. 调整 social_identity_source 列：去掉 issuer/scopes/claim_mapping，新增 type/mode
alter table social_identity_source drop column if exists issuer;
alter table social_identity_source drop column if exists scopes;
alter table social_identity_source drop column if exists claim_mapping;
alter table social_identity_source add column type varchar(32);
alter table social_identity_source add column mode varchar(32);

-- 旧 unique(tenant_id, issuer, client_id) 已随 issuer 列删除失效，需显式 drop
alter table social_identity_source drop constraint if exists federation_provider_tenant_id_issuer_client_id_key;

-- 清空旧 OIDC 配置数据（完全移除）
truncate social_identity_source, social_identity_binding, social_login_transaction restart identity cascade;

-- type 设为 not null（truncate 后无行，可安全加约束）
alter table social_identity_source alter column type set not null;
alter table social_identity_source add constraint social_identity_source_type_check
    check (type in ('wechat_qr', 'wechat_mp', 'github', 'gitee', 'feishu_web'));

-- 3. 调整 social_identity_binding 列：issuer -> source_type
alter table social_identity_binding rename column provider_id to source_id;
alter table social_identity_binding rename column issuer to source_type;
alter table social_identity_binding alter column source_type drop not null;
update social_identity_binding set source_type = null where source_type is not null;
alter table social_identity_binding alter column source_type set not null;

-- 4. 调整 social_login_transaction 列：provider_id -> source_id，pkce_verifier 可空
alter table social_login_transaction rename column provider_id to source_id;
alter table social_login_transaction alter column encrypted_pkce_verifier drop not null;

-- 5. 权限码重命名：social-identity-provider -> social-identity-source
-- management_permission 表有 BEFORE UPDATE OR DELETE 触发器禁止变更，需临时 drop。
drop trigger trg_management_permission_immutable on management_permission;

-- 先插入新权限码，使引用表更新时能够通过外键校验。
insert into management_permission(code, type, name, parent_code, resource, action, sort_order, active) values
 ('menu:social-identity-source', 'menu', '社会化身份源', 'directory:identity-source-management', 'social-identity-source', 'view', 31, true),
 ('social-identity-source:list', 'action', '查看社会化身份源列表', 'menu:social-identity-source', 'social-identity-source', 'list', 1, true),
 ('social-identity-source:stats', 'action', '查看社会化身份源统计', 'menu:social-identity-source', 'social-identity-source', 'stats', 2, true),
 ('social-identity-source:read', 'action', '查看社会化身份源详情', 'menu:social-identity-source', 'social-identity-source', 'read', 3, true),
 ('social-identity-source:create', 'action', '创建社会化身份源', 'menu:social-identity-source', 'social-identity-source', 'create', 4, true),
 ('social-identity-source:update', 'action', '更新社会化身份源', 'menu:social-identity-source', 'social-identity-source', 'update', 5, true),
 ('social-identity-source:delete', 'action', '删除社会化身份源', 'menu:social-identity-source', 'social-identity-source', 'delete', 6, true);

-- 再更新引用表，最后删除旧权限码（外键 on delete restrict）。
update admin_role_permission set permission_code = replace(permission_code, 'social-identity-provider', 'social-identity-source')
    where permission_code like 'social-identity-provider%';
update tenant_package_permission set permission_code = replace(permission_code, 'social-identity-provider', 'social-identity-source')
    where permission_code like 'social-identity-provider%';

delete from management_permission where code like 'social-identity-provider%';

-- 重建不可变触发器
create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();

-- 6. 清空旧 login_style.social_providers（旧值是类型字符串如 'oidc'，新架构存 source UUID 列表）
update login_style set social_providers = '[]'::jsonb where social_providers is distinct from '[]'::jsonb;
