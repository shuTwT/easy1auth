-- V16: 法律条款随登录样式草稿/发布管理，统一沿用 login-style 权限。
drop trigger if exists trg_management_permission_immutable on management_permission;

insert into admin_role_permission(role_id, permission_code, created_at)
select role_id, case permission_code
                    when 'legal-document:read' then 'login-style:read'
                    when 'legal-document:update' then 'login-style:update'
                end, created_at
from admin_role_permission
where permission_code in ('legal-document:read', 'legal-document:update')
on conflict (role_id, permission_code) do nothing;

insert into tenant_package_permission(package_id, permission_code, created_at)
select package_id, case permission_code
                       when 'legal-document:read' then 'login-style:read'
                       when 'legal-document:update' then 'login-style:update'
                   end, created_at
from tenant_package_permission
where permission_code in ('legal-document:read', 'legal-document:update')
on conflict (package_id, permission_code) do nothing;

delete from admin_role_permission where permission_code in ('legal-document:read', 'legal-document:update');
delete from tenant_package_permission where permission_code in ('legal-document:read', 'legal-document:update');
delete from management_permission where code in ('legal-document:read', 'legal-document:update');

create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();
