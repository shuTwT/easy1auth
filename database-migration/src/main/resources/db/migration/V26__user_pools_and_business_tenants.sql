-- Existing tenant / tenant_id storage identifies USER POOLS. Retain those physical names
-- for existing data, foreign keys, OAuth issuers and third-party token compatibility.
comment on table tenant is 'User pool: management, identity and subscription boundary; legacy physical name';
comment on table tenant_membership is 'Administrator membership in a user pool; legacy physical name';
comment on table tenant_package is 'User pool subscription and quota package; legacy physical name';

update tenant
set name = '系统用户池'
where is_system and name = '系统租户';

create table pool_tenant
(
    id           uuid primary key,
    user_pool_id uuid         not null references tenant (id) on delete cascade,
    name         varchar(200) not null check (length(btrim(name)) > 0),
    code         varchar(100) not null check (code ~ '^[a-z0-9_-]{1,100}$'),
    description  varchar(2000),
    status       varchar(32)  not null default 'active' check (status in ('active', 'inactive')),
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),
    unique (user_pool_id, code)
);
create index ix_pool_tenant_pool_created on pool_tenant (user_pool_id, created_at desc);
comment on table pool_tenant is 'Business tenants belonging to a user pool; metadata only, no management/authentication isolation';
comment on column pool_tenant.status is 'Metadata status only; does not affect administrator or pool-user login';

drop trigger if exists trg_management_permission_immutable on management_permission;

-- Keep stable permission values so existing packages and administrator grants still work.
update management_permission
set name = replace(name, '租户', '用户池')
where name like '%租户%';

insert into management_permission (code, type, name, parent_code, resource, action, sort_order, active)
values ('menu:pool-tenant', 'menu', '租户管理', null, 'pool-tenant', 'view', 39, true),
       ('pool-tenant:list', 'action', '查看租户列表', 'menu:pool-tenant', 'pool-tenant', 'list', 1, true),
       ('pool-tenant:read', 'action', '查看租户详情', 'menu:pool-tenant', 'pool-tenant', 'read', 2, true),
       ('pool-tenant:create', 'action', '创建租户', 'menu:pool-tenant', 'pool-tenant', 'create', 3, true),
       ('pool-tenant:update', 'action', '更新租户资料', 'menu:pool-tenant', 'pool-tenant', 'update', 4, true),
       ('pool-tenant:delete', 'action', '删除租户资料', 'menu:pool-tenant', 'pool-tenant', 'delete', 5, true);

-- Copy corresponding grants so read-only packages remain read-only.
insert into tenant_package_permission (package_id, permission_code, created_at)
select old.package_id, mapping.new_code, old.created_at
from tenant_package_permission old
join (values ('menu:user', 'menu:pool-tenant'), ('user:list', 'pool-tenant:list'),
             ('user:read', 'pool-tenant:read'), ('user:create', 'pool-tenant:create'),
             ('user:update', 'pool-tenant:update'), ('user:delete', 'pool-tenant:delete'))
     mapping(old_code, new_code) on old.permission_code = mapping.old_code
on conflict (package_id, permission_code) do nothing;

-- Copy action grants one by one; read-only roles do not gain write access.
insert into admin_role_permission (role_id, permission_code, created_at)
select old.role_id, mapping.new_code, old.created_at
from admin_role_permission old
join (values ('menu:user', 'menu:pool-tenant'), ('user:list', 'pool-tenant:list'),
             ('user:read', 'pool-tenant:read'), ('user:create', 'pool-tenant:create'),
             ('user:update', 'pool-tenant:update'), ('user:delete', 'pool-tenant:delete'))
     mapping(old_code, new_code) on old.permission_code = mapping.old_code
on conflict (role_id, permission_code) do nothing;

create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();
