create table admin_account (
    id uuid primary key,
    username varchar(100) not null unique,
    email varchar(320) not null unique,
    status varchar(32) not null default 'active' check (status in ('active','disabled','locked')),
    last_tenant_id uuid,
    security_version bigint not null default 1,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table tenant (
    id uuid primary key,
    name varchar(200) not null,
    status varchar(32) not null default 'active' check (status in ('active','suspended','deleted')),
    plan varchar(64) not null default 'basic',
    max_users integer not null default 100 check (max_users >= 0),
    max_apps integer not null default 10 check (max_apps >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table admin_account add constraint fk_admin_last_tenant foreign key(last_tenant_id) references tenant(id) on delete set null;

create table tenant_membership (
    id uuid primary key,
    account_id uuid not null references admin_account(id) on delete cascade,
    tenant_id uuid not null references tenant(id) on delete cascade,
    membership_role varchar(32) not null default 'member' check (membership_role in ('owner','admin','member')),
    status varchar(32) not null default 'active' check (status in ('active','suspended')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(account_id, tenant_id)
);
create unique index uq_tenant_single_owner on tenant_membership(tenant_id) where membership_role = 'owner' and status = 'active';
create index ix_membership_tenant on tenant_membership(tenant_id);

create table admin_role (
    id uuid primary key,
    tenant_id uuid not null references tenant(id) on delete cascade,
    name varchar(100) not null,
    description text,
    permissions jsonb not null default '[]'::jsonb,
    system_role boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(tenant_id, name)
);

create table membership_role_assignment (
    membership_id uuid not null references tenant_membership(id) on delete cascade,
    role_id uuid not null references admin_role(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key(membership_id, role_id)
);

comment on column admin_account.last_tenant_id is 'UI preference only; never an authorization source';
