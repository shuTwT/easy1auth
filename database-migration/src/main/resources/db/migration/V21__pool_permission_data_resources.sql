create table pool_permission_space
(
    id          uuid primary key,
    tenant_id   uuid         not null references tenant (id) on delete cascade,
    name        varchar(100) not null,
    code        varchar(100) not null,
    description text,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    unique (tenant_id, code),
    constraint ux_pool_permission_space_tenant_id_id unique (tenant_id, id)
);

alter table pool_permission
    add column space_id uuid,
    add column operations jsonb not null default '[]'::jsonb;

alter table pool_permission
    add constraint fk_pool_permission_space_tenant
        foreign key (tenant_id, space_id)
        references pool_permission_space (tenant_id, id);

create index ix_pool_permission_tenant_space on pool_permission (tenant_id, space_id);
