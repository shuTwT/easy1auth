create table security_policy (
 tenant_id uuid primary key references tenant(id) on delete cascade,
 password_min_length integer not null default 8 check(password_min_length between 8 and 128),
 password_require_upper boolean not null default true,
 password_require_lower boolean not null default true,
 password_require_number boolean not null default true,
 password_require_special boolean not null default true,
 password_max_age_days integer not null default 90 check(password_max_age_days between 0 and 3650),
 password_history_count integer not null default 5 check(password_history_count between 0 and 24),
 mfa_required boolean not null default false,
 login_attempt_limit integer not null default 5 check(login_attempt_limit between 1 and 100),
 lockout_duration_seconds integer not null default 1800 check(lockout_duration_seconds between 60 and 86400),
 updated_at timestamptz not null default now()
);

create table authentication_factor (
 id uuid primary key,
 subject_type varchar(16) not null check(subject_type in ('admin','pool_user')),
 subject_id uuid not null,
 tenant_id uuid references tenant(id) on delete cascade,
 factor_type varchar(16) not null check(factor_type in ('totp','email')),
 encrypted_secret text,
 enabled boolean not null default false,
 last_totp_step bigint,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 check((subject_type='admin' and tenant_id is null) or (subject_type='pool_user' and tenant_id is not null)),
 unique(subject_type, subject_id, factor_type)
);
create index ix_auth_factor_tenant on authentication_factor(tenant_id,subject_id);

create table authentication_recovery_code (
 id uuid primary key, factor_id uuid not null references authentication_factor(id) on delete cascade,
 code_hash char(64) not null, used_at timestamptz, created_at timestamptz not null default now(), unique(factor_id,code_hash)
);

create table authentication_challenge (
 id uuid primary key, token_hash char(64) not null unique,
 subject_type varchar(16) not null check(subject_type in ('admin','pool_user','registration')),
 subject_id uuid, tenant_id uuid references tenant(id) on delete cascade,
 purpose varchar(32) not null, factor_type varchar(16) not null,
 code_hash char(64), attempts integer not null default 0, max_attempts integer not null default 5,
 expires_at timestamptz not null, consumed_at timestamptz,
 created_at timestamptz not null default now(), last_sent_at timestamptz
);
create index ix_auth_challenge_subject on authentication_challenge(subject_type,subject_id,created_at desc);
create index ix_auth_challenge_expiry on authentication_challenge(expires_at) where consumed_at is null;

create table login_failure_state (
 id uuid primary key,
 subject_type varchar(16) not null, subject_key varchar(400) not null, tenant_id uuid,
 failed_attempts integer not null default 0, locked_until timestamptz, last_failed_at timestamptz
);
create unique index uq_login_failure_subject on login_failure_state(subject_type,subject_key,coalesce(tenant_id,'00000000-0000-0000-0000-000000000000'::uuid));

create table password_history (
 id uuid primary key, subject_type varchar(16) not null, subject_id uuid not null,
 password_hash varchar(100) not null, created_at timestamptz not null default now()
);
create index ix_password_history_subject on password_history(subject_type,subject_id,created_at desc);

create table pool_user_device (
 id uuid primary key, tenant_id uuid not null references tenant(id) on delete cascade,
 user_id uuid not null references pool_user(id) on delete cascade, device_token_hash char(64) not null,
 user_agent varchar(512), ip_address varchar(64), first_seen_at timestamptz not null default now(),
 last_seen_at timestamptz not null default now(), revoked_at timestamptz,
 unique(tenant_id,user_id,device_token_hash)
);

create table federation_provider (
 id uuid primary key, tenant_id uuid not null references tenant(id) on delete cascade,
 name varchar(120) not null, issuer varchar(1000) not null, client_id varchar(500) not null,
 encrypted_client_secret text not null, scopes jsonb not null default '["openid","profile","email"]'::jsonb,
 claim_mapping jsonb not null default '{}'::jsonb, jit_provisioning boolean not null default false,
 status varchar(16) not null default 'active' check(status in ('active','disabled')),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 unique(tenant_id,name), unique(tenant_id,issuer,client_id)
);
create table external_identity_binding (
 id uuid primary key, tenant_id uuid not null references tenant(id) on delete cascade,
 provider_id uuid not null references federation_provider(id) on delete cascade,
 pool_user_id uuid not null references pool_user(id) on delete cascade,
 issuer varchar(1000) not null, subject varchar(1000) not null, claims jsonb not null default '{}'::jsonb,
 created_at timestamptz not null default now(), last_login_at timestamptz,
 unique(tenant_id,provider_id,issuer,subject), unique(tenant_id,provider_id,pool_user_id)
);
create table federation_login_transaction (
 id uuid primary key, tenant_id uuid not null references tenant(id) on delete cascade,
 provider_id uuid not null references federation_provider(id) on delete cascade,
 state_hash char(64) not null unique, nonce_hash char(64) not null, encrypted_nonce text not null, encrypted_pkce_verifier text not null,
 return_uri text, expires_at timestamptz not null, consumed_at timestamptz, created_at timestamptz not null default now()
);

create table brand_setting (
 tenant_id uuid primary key references tenant(id) on delete cascade,
 settings jsonb not null default '{}'::jsonb,
 company_name varchar(200), logo text, favicon text, primary_color varchar(16) not null default '#0369A1',
 secondary_color varchar(16), support_email varchar(320), copyright_text varchar(500),
 updated_at timestamptz not null default now()
);
create table login_style (
 tenant_id uuid primary key references tenant(id) on delete cascade,
 logo text, logo_dark text, background_image text, background_color varchar(16) not null default '#f5f7fa',
 primary_color varchar(16) not null default '#0369A1', title varchar(200) not null default 'Easy1Auth',
 subtitle varchar(500) not null default '企业级身份管理平台', custom_css text,
 login_methods jsonb not null default '["password"]'::jsonb, social_providers jsonb not null default '[]'::jsonb,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table custom_domain (
 id uuid primary key, tenant_id uuid not null references tenant(id) on delete cascade,
 domain varchar(253) not null unique, status varchar(16) not null default 'pending' check(status in ('pending','verified','disabled')),
 verification_method varchar(16) not null default 'dns' check(verification_method in ('dns','file')),
 verification_token varchar(200) not null, verified_at timestamptz,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table message_template (
 id uuid primary key, tenant_id uuid not null references tenant(id) on delete cascade,
 type varchar(16) not null check(type in ('email','sms')), code varchar(100) not null, name varchar(200) not null,
 subject varchar(500), content text not null, variables jsonb not null default '{}'::jsonb,
 is_default boolean not null default false, status varchar(16) not null default 'active' check(status in ('active','inactive')),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(), unique(tenant_id,type,code)
);

create table audit_event (
 id uuid primary key, tenant_id uuid references tenant(id) on delete cascade,
 actor_type varchar(24) not null, actor_id uuid, actor_name varchar(320), event_type varchar(100) not null,
 action varchar(100) not null, resource_type varchar(100) not null, resource_id varchar(200),
 trace_id varchar(100), method varchar(16), ip_address varchar(64), user_agent varchar(512),
 outcome varchar(16) not null check(outcome in ('success','failure')), error_code varchar(100),
 details jsonb not null default '{}'::jsonb, created_at timestamptz not null default now()
);
create index ix_audit_tenant_time on audit_event(tenant_id,created_at desc);
create index ix_audit_actor on audit_event(actor_type,actor_id,created_at desc);

create table webhook_subscription (
 id uuid primary key, tenant_id uuid not null references tenant(id) on delete cascade,
 name varchar(200) not null, url text not null, events jsonb not null, secret_hash char(64) not null,
 encrypted_secret text not null, status varchar(16) not null default 'active' check(status in ('active','disabled')),
 max_retries integer not null default 5 check(max_retries between 0 and 20),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now(), unique(tenant_id,name)
);
create table delivery_outbox (
 id uuid primary key, tenant_id uuid references tenant(id) on delete cascade,
 channel varchar(16) not null check(channel in ('email','webhook')), destination text not null,
 event_type varchar(100) not null, payload jsonb not null, subscription_id uuid references webhook_subscription(id) on delete cascade,
 idempotency_key varchar(200) not null unique, status varchar(16) not null default 'pending' check(status in ('pending','processing','sent','dead')),
 attempts integer not null default 0, max_attempts integer not null default 5,
 available_at timestamptz not null default now(), lease_until timestamptz, last_error varchar(1000),
 created_at timestamptz not null default now(), sent_at timestamptz
);
create index ix_delivery_claim on delivery_outbox(status,available_at) where status in ('pending','processing');
create table delivery_attempt (
 id uuid primary key, outbox_id uuid not null references delivery_outbox(id) on delete cascade,
 attempt_number integer not null, response_status integer, error_message varchar(1000),
 attempted_at timestamptz not null default now(), unique(outbox_id,attempt_number)
);

comment on column login_style.custom_css is 'Compatibility storage only; never rendered on authentication pages';
comment on table custom_domain is 'Phase 6 inventory only; ownership verification and TLS are external/deferred';
