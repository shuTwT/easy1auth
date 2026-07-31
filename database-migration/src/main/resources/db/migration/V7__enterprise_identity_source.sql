create table enterprise_identity_source (
    id uuid primary key,
    tenant_id uuid not null references tenant(id) on delete cascade,
    name varchar(120) not null,
    provider varchar(32) not null check (provider in ('feishu')),
    app_id varchar(200) not null,
    encrypted_app_secret text not null,
    encrypted_verification_token text not null,
    encrypted_encrypt_key text not null,
    status varchar(16) not null default 'active' check (status in ('active', 'disabled')),
    last_sync_at timestamptz,
    last_sync_status varchar(32),
    last_error varchar(1000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (tenant_id, name), unique (tenant_id, provider, app_id)
);

create table enterprise_identity_sync_task (
    id uuid primary key,
    tenant_id uuid not null references tenant(id) on delete cascade,
    source_id uuid not null references enterprise_identity_source(id) on delete cascade,
    type varchar(32) not null check (type in ('full', 'event')),
    event_id varchar(200),
    payload jsonb not null default '{}'::jsonb,
    status varchar(16) not null default 'pending' check (status in ('pending', 'processing', 'succeeded', 'partial', 'failed')),
    summary jsonb not null default '{}'::jsonb,
    last_error varchar(1000),
    created_at timestamptz not null default now(),
    started_at timestamptz, finished_at timestamptz,
    unique (source_id, event_id)
);
create index ix_enterprise_identity_task_claim on enterprise_identity_sync_task(status, created_at) where status = 'pending';

alter table pool_user add column enterprise_identity_source_id uuid references enterprise_identity_source(id) on delete set null;
alter table pool_user add column enterprise_identity_external_id varchar(200);
create unique index uq_pool_user_enterprise_identity_external on pool_user(enterprise_identity_source_id, enterprise_identity_external_id) where enterprise_identity_source_id is not null;

alter table user_group add column enterprise_identity_source_id uuid references enterprise_identity_source(id) on delete set null;
alter table user_group add column enterprise_identity_external_id varchar(200);
create unique index uq_user_group_enterprise_identity_external on user_group(enterprise_identity_source_id, enterprise_identity_external_id) where enterprise_identity_source_id is not null;
alter table user_group drop constraint if exists user_group_tenant_id_name_key;
create unique index uq_user_group_tenant_parent_name on user_group(tenant_id, coalesce(parent_id, '00000000-0000-0000-0000-000000000000'::uuid), name);

drop trigger if exists trg_management_permission_immutable on management_permission;
insert into management_permission(code, type, name, parent_code, resource, action, sort_order, active) values
 ('directory:identity-source-management', 'directory', '身份源管理', null, 'identity-source-management', 'view', 31, true),
 ('menu:enterprise-identity-source', 'menu', '企业身份源', 'directory:identity-source-management', 'enterprise-identity-source', 'view', 32, true),
 ('enterprise-identity-source:list', 'action', '查看企业身份源列表', 'menu:enterprise-identity-source', 'enterprise-identity-source', 'list', 1, true),
 ('enterprise-identity-source:stats', 'action', '查看企业身份源统计', 'menu:enterprise-identity-source', 'enterprise-identity-source', 'stats', 2, true),
 ('enterprise-identity-source:read', 'action', '查看企业身份源详情', 'menu:enterprise-identity-source', 'enterprise-identity-source', 'read', 3, true),
 ('enterprise-identity-source:create', 'action', '创建企业身份源', 'menu:enterprise-identity-source', 'enterprise-identity-source', 'create', 4, true),
 ('enterprise-identity-source:update', 'action', '更新企业身份源', 'menu:enterprise-identity-source', 'enterprise-identity-source', 'update', 5, true),
 ('enterprise-identity-source:delete', 'action', '删除企业身份源', 'menu:enterprise-identity-source', 'enterprise-identity-source', 'delete', 6, true),
 ('enterprise-identity-source:sync', 'action', '同步企业身份源', 'menu:enterprise-identity-source', 'enterprise-identity-source', 'sync', 7, true);
update management_permission set parent_code = 'directory:identity-source-management' where code = 'menu:social-identity-provider';
create trigger trg_management_permission_immutable before update or delete on management_permission for each row execute function reject_management_permission_change();

insert into tenant_package_permission(package_id, permission_code)
select p.id, x.code from tenant_package p cross join (values
 ('directory:identity-source-management'), ('menu:enterprise-identity-source'),
 ('enterprise-identity-source:list'), ('enterprise-identity-source:stats'), ('enterprise-identity-source:read'),
 ('enterprise-identity-source:create'), ('enterprise-identity-source:update'), ('enterprise-identity-source:delete'), ('enterprise-identity-source:sync')
) as x(code) where p.code = 'basic' on conflict do nothing;
