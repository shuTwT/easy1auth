-- V13: 将品牌设置与个性化设置合并为品牌管理目录。
-- 旧菜单保留为 inactive，避免历史权限数据失效；新的目录和子菜单成为唯一可见入口。

drop trigger if exists trg_management_permission_immutable on management_permission;

insert into management_permission(code, type, name, parent_code, resource, action, sort_order, active)
values
    ('directory:brand-management', 'directory', '品牌管理', null, 'brand-management', 'view', 41, true),
    ('menu:brand-login-style', 'menu', '个性化登录页面', 'directory:brand-management', 'brand-login-style', 'view', 41, true),
    ('menu:message-service', 'menu', '消息服务', 'directory:brand-management', 'message-service', 'view', 42, true),
    ('menu:custom-domain', 'menu', '自定义域名', 'directory:brand-management', 'custom-domain', 'view', 43, true),
    ('menu:admin-panel', 'menu', '管理面板', 'directory:brand-management', 'admin-panel', 'view', 44, true)
on conflict (code) do update set
    type = excluded.type,
    name = excluded.name,
    parent_code = excluded.parent_code,
    resource = excluded.resource,
    action = excluded.action,
    sort_order = excluded.sort_order,
    active = excluded.active;

-- 原品牌/个性化菜单的授权迁移到品牌管理及其全部子菜单，保持已有管理员可见性。
insert into admin_role_permission(role_id, permission_code, created_at)
select distinct old.role_id, new_code.code, old.created_at
from admin_role_permission old
cross join (values
    ('directory:brand-management'),
    ('menu:brand-login-style'),
    ('menu:message-service'),
    ('menu:custom-domain'),
    ('menu:admin-panel')
) as new_code(code)
where old.permission_code in ('menu:brand-settings', 'menu:personalization')
on conflict (role_id, permission_code) do nothing;

insert into tenant_package_permission(package_id, permission_code, created_at)
select distinct old.package_id, new_code.code, old.created_at
from tenant_package_permission old
cross join (values
    ('directory:brand-management'),
    ('menu:brand-login-style'),
    ('menu:message-service'),
    ('menu:custom-domain'),
    ('menu:admin-panel')
) as new_code(code)
where old.permission_code in ('menu:brand-settings', 'menu:personalization')
on conflict (package_id, permission_code) do nothing;

delete from admin_role_permission
where permission_code in ('menu:brand-settings', 'menu:personalization');
delete from tenant_package_permission
where permission_code in ('menu:brand-settings', 'menu:personalization');

-- 动作权限改挂到新的业务子菜单。
update management_permission
set parent_code = case
    when code in ('login-style:read', 'login-style:update') then 'menu:brand-login-style'
    when code like 'custom-domain:%' then 'menu:custom-domain'
    when code like 'message-template:%' then 'menu:message-service'
    when code in ('brand:read', 'brand:update') then 'directory:brand-management'
    else parent_code
end
where code in ('login-style:read', 'login-style:update', 'brand:read', 'brand:update')
   or code like 'custom-domain:%'
   or code like 'message-template:%';

update management_permission
set active = false
where code in ('menu:brand-settings', 'menu:personalization');

create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();
