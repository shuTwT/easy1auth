alter table admin_account add column phone varchar(32);
alter table admin_account add column mfa_enabled boolean not null default false;
alter table admin_account add column mfa_type varchar(32);
alter table admin_account add column last_login_at timestamptz;

create table admin_credential (
    account_id uuid primary key references admin_account(id) on delete cascade,
    password_hash varchar(100) not null,
    password_changed_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table admin_refresh_session (
    id uuid primary key,
    account_id uuid not null references admin_account(id) on delete cascade,
    token_hash char(64) not null unique,
    security_version bigint not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    replaced_by uuid,
    created_at timestamptz not null default now(),
    last_used_at timestamptz,
    user_agent varchar(512),
    ip_address varchar(64)
);
create index ix_admin_refresh_account on admin_refresh_session(account_id);
create index ix_admin_refresh_expiry on admin_refresh_session(expires_at) where revoked_at is null;

create table admin_mfa_secret (
    account_id uuid primary key references admin_account(id) on delete cascade,
    encrypted_secret text not null,
    backup_code_hashes jsonb not null default '[]'::jsonb,
    updated_at timestamptz not null default now()
);

create table admin_registration_code (
    id uuid primary key,
    email varchar(320) not null,
    code_hash char(64) not null,
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    unique(email, code_hash)
);
create index ix_admin_registration_expiry on admin_registration_code(expires_at);
