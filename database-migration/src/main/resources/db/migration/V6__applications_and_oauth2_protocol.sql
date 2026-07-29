create table oauth_application (
    id uuid primary key,
    tenant_id uuid not null references tenant(id) on delete cascade,
    name varchar(200) not null,
    logo text,
    description text,
    type varchar(32) not null default 'web' check(type in('web','native','spa','machine')),
    client_id varchar(100) not null unique,
    client_secret_hash varchar(200),
    redirect_uris jsonb not null default '[]'::jsonb,
    post_logout_redirect_uris jsonb not null default '[]'::jsonb,
    allowed_grant_types jsonb not null default '["authorization_code"]'::jsonb,
    scopes jsonb not null default '["openid","profile","email","phone"]'::jsonb,
    require_pkce boolean not null default true,
    require_consent boolean not null default true,
    access_token_lifetime integer not null default 900 check(access_token_lifetime between 60 and 86400),
    refresh_token_lifetime integer not null default 2592000 check(refresh_token_lifetime between 300 and 31536000),
    status varchar(32) not null default 'active' check(status in('active','disabled')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(tenant_id,name),
    unique(tenant_id,id)
);
create index ix_oauth_application_tenant on oauth_application(tenant_id);

create table oauth2_authorization (
    id varchar(100) primary key,
    tenant_id uuid not null references tenant(id) on delete cascade,
    registered_client_id varchar(100) not null,
    principal_name varchar(200) not null,
    authorization_grant_type varchar(100) not null,
    authorized_scopes text,
    attributes text,
    state varchar(500),
    authorization_code_value text,
    authorization_code_issued_at timestamptz,
    authorization_code_expires_at timestamptz,
    authorization_code_metadata text,
    access_token_value text,
    access_token_issued_at timestamptz,
    access_token_expires_at timestamptz,
    access_token_metadata text,
    access_token_type varchar(100),
    access_token_scopes text,
    refresh_token_value text,
    refresh_token_issued_at timestamptz,
    refresh_token_expires_at timestamptz,
    refresh_token_metadata text,
    oidc_id_token_value text,
    oidc_id_token_issued_at timestamptz,
    oidc_id_token_expires_at timestamptz,
    oidc_id_token_metadata text,
    oidc_id_token_claims text,
    user_code_value text,
    user_code_issued_at timestamptz,
    user_code_expires_at timestamptz,
    user_code_metadata text,
    device_code_value text,
    device_code_issued_at timestamptz,
    device_code_expires_at timestamptz,
    device_code_metadata text
);
create index ix_oauth2_authorization_tenant_client on oauth2_authorization(tenant_id,registered_client_id);
create index ix_oauth2_authorization_state on oauth2_authorization(state);

create table oauth2_authorization_consent (
    tenant_id uuid not null references tenant(id) on delete cascade,
    registered_client_id varchar(100) not null,
    principal_name varchar(200) not null,
    authorities text not null,
    primary key(tenant_id,registered_client_id,principal_name)
);

create function resolve_oauth_tenant(client varchar) returns uuid language sql stable as $$
    select tenant_id from oauth_application where id::text=client
$$;
create function set_oauth_authorization_tenant() returns trigger language plpgsql as $$
begin
    new.tenant_id:=resolve_oauth_tenant(new.registered_client_id);
    if new.tenant_id is null then raise exception 'unknown oauth registered client'; end if;
    return new;
end $$;
create trigger trg_oauth2_authorization_tenant before insert or update of registered_client_id on oauth2_authorization
    for each row execute function set_oauth_authorization_tenant();
create trigger trg_oauth2_consent_tenant before insert or update of registered_client_id on oauth2_authorization_consent
    for each row execute function set_oauth_authorization_tenant();
create function cleanup_oauth_application_protocol() returns trigger language plpgsql as $$
begin
    delete from oauth2_authorization_consent where registered_client_id=old.id::text;
    delete from oauth2_authorization where registered_client_id=old.id::text;
    return old;
end $$;
create trigger trg_oauth_application_protocol_cleanup before delete on oauth_application
    for each row execute function cleanup_oauth_application_protocol();

create table oauth2_signing_key (
    id uuid primary key,
    tenant_id uuid not null references tenant(id) on delete cascade,
    key_id varchar(100) not null,
    algorithm varchar(32) not null default 'RS256',
    public_jwk jsonb not null,
    encrypted_private_jwk text not null,
    status varchar(32) not null default 'active' check(status in('active','retired')),
    created_at timestamptz not null default now(),
    expires_at timestamptz,
    unique(tenant_id,key_id)
);
create unique index uq_oauth2_active_signing_key on oauth2_signing_key(tenant_id) where status='active';
