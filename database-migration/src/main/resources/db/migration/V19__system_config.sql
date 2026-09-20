create table sys_config
(
    config_key   varchar(150) primary key,
    config_value text         not null,
    description  varchar(500),
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),
    constraint ck_sys_config_key_not_blank check (btrim(config_key) <> '')
);

comment on table sys_config is '系统级配置参数（不隶属于租户）';
comment on column sys_config.config_key is '稳定且唯一的配置键';
comment on column sys_config.config_value is '配置值，由 system 模块负责类型转换';

create unique index uq_tenant_single_system
    on tenant (is_system) where is_system;

insert into sys_config (config_key, config_value, description)
select 'system.initialized',
       case
           when exists(select 1 from admin_account)
               or exists(select 1 from tenant where is_system)
               then 'true'
           else 'false'
           end,
       '系统初始化是否已完成';
