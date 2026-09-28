-- 补齐管理员角色编码，保留原有角色 ID、权限及成员关联。
alter table admin_role add column code varchar(100);
update admin_role
set code = case
    when system_role and name = '超级管理员' then 'super_admin'
    when system_role and name = '只读管理员' then 'read_only_admin'
    else 'role_' || replace(id::text, '-', '')
end;
alter table admin_role alter column code set not null;
alter table admin_role add constraint ck_admin_role_code
    check (code ~ '^[a-z][a-z0-9_]{1,99}$');
alter table admin_role add constraint uq_admin_role_tenant_code
    unique (tenant_id, code);
