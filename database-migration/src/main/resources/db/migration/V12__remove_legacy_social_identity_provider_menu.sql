-- V11 将 social-identity-provider:* 动作权限迁移到 social-identity-source:*，
-- 但旧菜单码以 menu: 开头，不匹配当时的 social-identity-provider% 条件。

drop trigger if exists trg_management_permission_immutable on management_permission;

-- 先复制引用并忽略已存在的新菜单权限，再删除旧引用。
insert into admin_role_permission(role_id, permission_code, created_at)
select role_id, 'menu:social-identity-source', created_at
from admin_role_permission
where permission_code = 'menu:social-identity-provider'
on conflict (role_id, permission_code) do nothing;

delete from admin_role_permission
where permission_code = 'menu:social-identity-provider';

insert into tenant_package_permission(package_id, permission_code, created_at)
select package_id, 'menu:social-identity-source', created_at
from tenant_package_permission
where permission_code = 'menu:social-identity-provider'
on conflict (package_id, permission_code) do nothing;

delete from tenant_package_permission
where permission_code = 'menu:social-identity-provider';

delete from management_permission
where code = 'menu:social-identity-provider';

create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();
