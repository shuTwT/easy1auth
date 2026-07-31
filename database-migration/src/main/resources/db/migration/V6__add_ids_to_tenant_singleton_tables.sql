alter table brand_setting
    add column id uuid;
update brand_setting
set id = tenant_id;
alter table brand_setting
    alter column id set not null,
    drop constraint brand_setting_pkey,
    add constraint brand_setting_pkey primary key (id),
    add constraint uq_brand_setting_tenant unique (tenant_id);

alter table login_style
    add column id uuid;
update login_style
set id = tenant_id;
alter table login_style
    alter column id set not null,
    drop constraint login_style_pkey,
    add constraint login_style_pkey primary key (id),
    add constraint uq_login_style_tenant unique (tenant_id);

alter table security_policy
    add column id uuid;
update security_policy
set id = tenant_id;
alter table security_policy
    alter column id set not null,
    drop constraint security_policy_pkey,
    add constraint security_policy_pkey primary key (id),
    add constraint uq_security_policy_tenant unique (tenant_id);
