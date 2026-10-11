-- 原“权限管理”页面同时承载权限空间与数据资源两块内容，现拆分为“权限管理”目录下的
-- 两个二级菜单，并为权限空间补充独立的操作权限码。

drop trigger if exists trg_management_permission_immutable on management_permission;

-- “权限管理”菜单升级为目录，下辖“权限空间管理”与“数据资源管理”。
insert into management_permission (code, type, name, parent_code, resource, action, sort_order, active)
values ('directory:permission-management', 'directory', '权限管理', null, 'permission-management', 'view', 25, true),
       ('menu:permission-space', 'menu', '权限空间管理', 'directory:permission-management', 'permission-space', 'view', 25, true),
       ('menu:permission-data-resource', 'menu', '数据资源管理', 'directory:permission-management', 'permission-data-resource', 'view', 26, true);

-- 权限空间不再复用数据资源权限码，改为拥有自己的操作权限。
insert into management_permission (code, type, name, parent_code, resource, action, sort_order, active)
values ('permission-space:list', 'action', '查看权限空间列表', 'menu:permission-space', 'permission-space', 'list', 1, true),
       ('permission-space:create', 'action', '创建权限空间', 'menu:permission-space', 'permission-space', 'create', 2, true),
       ('permission-space:update', 'action', '更新权限空间', 'menu:permission-space', 'permission-space', 'update', 3, true),
       ('permission-space:delete', 'action', '删除权限空间', 'menu:permission-space', 'permission-space', 'delete', 4, true);

-- 数据资源动作挂到“数据资源管理”菜单下，名称统一为数据资源口径。
update management_permission
set parent_code = 'menu:permission-data-resource',
    name        = replace(name, '数据权限', '数据资源')
where parent_code = 'menu:permission';

-- 套餐授权迁移：原菜单授权转给新目录与两个菜单；原来能用数据资源动作的套餐，
-- 同步获得等价的权限空间动作，数据资源管理页读取权限空间列表也依赖这份授权。
insert into tenant_package_permission (package_id, permission_code, created_at)
select old.package_id, mapping.new_code, old.created_at
from tenant_package_permission old
         join (values ('menu:permission', 'directory:permission-management'),
                      ('menu:permission', 'menu:permission-space'),
                      ('menu:permission', 'menu:permission-data-resource'),
                      ('data-permission:list', 'permission-space:list'),
                      ('data-permission:create', 'permission-space:create'),
                      ('data-permission:update', 'permission-space:update'),
                      ('data-permission:delete', 'permission-space:delete'))
             mapping(old_code, new_code) on old.permission_code = mapping.old_code
on conflict (package_id, permission_code) do nothing;

insert into admin_role_permission (role_id, permission_code, created_at)
select old.role_id, mapping.new_code, old.created_at
from admin_role_permission old
         join (values ('menu:permission', 'directory:permission-management'),
                      ('menu:permission', 'menu:permission-space'),
                      ('menu:permission', 'menu:permission-data-resource'),
                      ('data-permission:list', 'permission-space:list'),
                      ('data-permission:create', 'permission-space:create'),
                      ('data-permission:update', 'permission-space:update'),
                      ('data-permission:delete', 'permission-space:delete'))
             mapping(old_code, new_code) on old.permission_code = mapping.old_code
on conflict (role_id, permission_code) do nothing;

-- 页面可见即应可打开：菜单授权到位时补齐对应的列表权限，避免菜单能显示但列表报错。
insert into tenant_package_permission (package_id, permission_code)
select granted.package_id, mapping.view_code
from tenant_package_permission granted
         join (values ('menu:permission-space', 'permission-space:list'),
                      ('menu:permission-data-resource', 'data-permission:list'))
             mapping(menu_code, view_code) on granted.permission_code = mapping.menu_code
on conflict (package_id, permission_code) do nothing;

insert into admin_role_permission (role_id, permission_code)
select granted.role_id, mapping.view_code
from admin_role_permission granted
         join (values ('menu:permission-space', 'permission-space:list'),
                      ('menu:permission-data-resource', 'data-permission:list'))
             mapping(menu_code, view_code) on granted.permission_code = mapping.menu_code
on conflict (role_id, permission_code) do nothing;

delete from tenant_package_permission where permission_code = 'menu:permission';
delete from admin_role_permission where permission_code = 'menu:permission';
delete from management_permission where code = 'menu:permission';

create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();
