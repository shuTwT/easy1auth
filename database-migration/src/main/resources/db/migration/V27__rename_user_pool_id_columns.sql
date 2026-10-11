-- Rename in place: row values, foreign keys, composite keys and indexes remain intact.
-- Historical tenant tables still identify user pools; pool_tenant is the business tenant table.
do $$
declare
    target record;
begin
    for target in
        select columns.table_schema, columns.table_name
        from information_schema.columns columns
        join information_schema.tables tables
          on tables.table_schema = columns.table_schema and tables.table_name = columns.table_name
        where columns.table_schema = current_schema()
          and columns.column_name = 'tenant_id'
          and tables.table_type = 'BASE TABLE'
        order by columns.table_name
    loop
        execute format('alter table %I.%I rename column tenant_id to user_pool_id',
                       target.table_schema, target.table_name);
    end loop;
end $$;

alter table admin_account rename column last_tenant_id to last_user_pool_id;
comment on column admin_account.last_user_pool_id is 'Last selected user pool; UI preference only, never an authorization source';

-- Function bodies are SQL text, so PostgreSQL column renames do not rewrite them.
drop trigger trg_oauth2_authorization_tenant on oauth2_authorization;
drop trigger trg_oauth2_consent_tenant on oauth2_authorization_consent;
drop function set_oauth_authorization_tenant();
drop function resolve_oauth_tenant(varchar);

create function resolve_oauth_user_pool(client varchar) returns uuid
    language sql stable as $$
    select user_pool_id from oauth_application where id::text = client
$$;

create function set_oauth_authorization_user_pool() returns trigger
    language plpgsql as $$
begin
    new.user_pool_id := resolve_oauth_user_pool(new.registered_client_id);
    if new.user_pool_id is null then
        raise exception 'unknown oauth registered client';
    end if;
    return new;
end $$;

create trigger trg_oauth2_authorization_user_pool
    before insert or update of registered_client_id on oauth2_authorization
    for each row execute function set_oauth_authorization_user_pool();

create trigger trg_oauth2_consent_user_pool
    before insert or update of registered_client_id on oauth2_authorization_consent
    for each row execute function set_oauth_authorization_user_pool();
