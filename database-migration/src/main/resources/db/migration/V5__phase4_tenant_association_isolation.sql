create unique index ux_pool_user_tenant_id_id on pool_user(tenant_id,id);
create unique index ux_user_group_tenant_id_id on user_group(tenant_id,id);
create unique index ux_pool_role_tenant_id_id on pool_role(tenant_id,id);
create unique index ux_pool_permission_tenant_id_id on pool_permission(tenant_id,id);

alter table user_group add constraint fk_user_group_parent_tenant
    foreign key(tenant_id,parent_id) references user_group(tenant_id,id);
alter table position add constraint fk_position_department_tenant
    foreign key(tenant_id,department_id) references user_group(tenant_id,id);
alter table pool_role add constraint fk_pool_role_parent_tenant
    foreign key(tenant_id,parent_id) references pool_role(tenant_id,id);
alter table pool_permission add constraint fk_pool_permission_parent_tenant
    foreign key(tenant_id,parent_id) references pool_permission(tenant_id,id);

alter table pool_user_group add column tenant_id uuid;
update pool_user_group a set tenant_id=u.tenant_id from pool_user u where u.id=a.user_id;
do $$ begin
    if exists(select 1 from pool_user_group a join user_group g on g.id=a.group_id where a.tenant_id<>g.tenant_id) then
        raise exception 'cross-tenant pool_user_group data exists';
    end if;
end $$;
alter table pool_user_group alter column tenant_id set not null;
alter table pool_user_group drop constraint pool_user_group_pkey;
alter table pool_user_group add primary key(tenant_id,user_id,group_id);
alter table pool_user_group add constraint fk_pool_user_group_user_tenant
    foreign key(tenant_id,user_id) references pool_user(tenant_id,id) on delete cascade;
alter table pool_user_group add constraint fk_pool_user_group_group_tenant
    foreign key(tenant_id,group_id) references user_group(tenant_id,id) on delete cascade;

alter table pool_group_admin add column tenant_id uuid;
update pool_group_admin a set tenant_id=u.tenant_id from pool_user u where u.id=a.user_id;
do $$ begin
    if exists(select 1 from pool_group_admin a join user_group g on g.id=a.group_id where a.tenant_id<>g.tenant_id) then
        raise exception 'cross-tenant pool_group_admin data exists';
    end if;
end $$;
alter table pool_group_admin alter column tenant_id set not null;
alter table pool_group_admin drop constraint pool_group_admin_pkey;
alter table pool_group_admin add primary key(tenant_id,group_id,user_id);
alter table pool_group_admin add constraint fk_pool_group_admin_user_tenant
    foreign key(tenant_id,user_id) references pool_user(tenant_id,id) on delete cascade;
alter table pool_group_admin add constraint fk_pool_group_admin_group_tenant
    foreign key(tenant_id,group_id) references user_group(tenant_id,id) on delete cascade;

alter table pool_user_role add column tenant_id uuid;
update pool_user_role a set tenant_id=u.tenant_id from pool_user u where u.id=a.user_id;
do $$ begin
    if exists(select 1 from pool_user_role a join pool_role r on r.id=a.role_id where a.tenant_id<>r.tenant_id) then
        raise exception 'cross-tenant pool_user_role data exists';
    end if;
end $$;
alter table pool_user_role alter column tenant_id set not null;
alter table pool_user_role drop constraint pool_user_role_pkey;
alter table pool_user_role add primary key(tenant_id,user_id,role_id);
alter table pool_user_role add constraint fk_pool_user_role_user_tenant
    foreign key(tenant_id,user_id) references pool_user(tenant_id,id) on delete cascade;
alter table pool_user_role add constraint fk_pool_user_role_role_tenant
    foreign key(tenant_id,role_id) references pool_role(tenant_id,id) on delete cascade;
