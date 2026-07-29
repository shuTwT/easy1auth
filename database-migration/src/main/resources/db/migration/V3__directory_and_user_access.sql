create table pool_user (
 id uuid primary key, tenant_id uuid not null references tenant(id) on delete cascade,
 username varchar(100) not null, email varchar(320) not null, phone varchar(32), password_hash varchar(100), name varchar(200) not null,
 avatar text, status varchar(32) not null default 'active' check(status in('active','disabled','locked')),
 email_verified boolean not null default false, phone_verified boolean not null default false,
 department varchar(200), position varchar(200), custom_attributes jsonb,
 last_login_at timestamptz, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 unique(tenant_id,username), unique(tenant_id,email)
);
create index ix_pool_user_tenant on pool_user(tenant_id);
create table user_group(id uuid primary key,tenant_id uuid not null references tenant(id) on delete cascade,name varchar(200) not null,description text,type varchar(32) not null default 'team',parent_id uuid references user_group(id),created_at timestamptz not null default now(),updated_at timestamptz not null default now(),unique(tenant_id,name));
create table position(id uuid primary key,tenant_id uuid not null references tenant(id) on delete cascade,name varchar(200) not null,code varchar(100) not null,description text,department_id uuid,level integer not null default 1,sequence varchar(100),max_count integer,created_at timestamptz not null default now(),updated_at timestamptz not null default now(),unique(tenant_id,code));
create table pool_role(id uuid primary key,tenant_id uuid not null references tenant(id) on delete cascade,name varchar(100) not null,code varchar(100) not null,description text,type varchar(32) not null default 'custom',permissions jsonb not null default '{}'::jsonb,data_scope varchar(32) not null default 'self',parent_id uuid references pool_role(id),created_at timestamptz not null default now(),updated_at timestamptz not null default now(),unique(tenant_id,code));
create table pool_permission(id uuid primary key,tenant_id uuid not null references tenant(id) on delete cascade,code varchar(150) not null,name varchar(150) not null,description text,type varchar(32) not null default 'operation',parent_id uuid references pool_permission(id),resource varchar(150) not null,action varchar(100) not null,created_at timestamptz not null default now(),updated_at timestamptz not null default now(),unique(tenant_id,code));
create table pool_user_group(user_id uuid not null references pool_user(id) on delete cascade,group_id uuid not null references user_group(id) on delete cascade,primary key(user_id,group_id));
create table pool_user_role(user_id uuid not null references pool_user(id) on delete cascade,role_id uuid not null references pool_role(id) on delete cascade,primary key(user_id,role_id));
