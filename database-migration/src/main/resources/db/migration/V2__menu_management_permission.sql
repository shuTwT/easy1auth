insert into management_permission (code, type, name, parent_code, resource, action, sort_order, active)
values ('menu:tenant-package', 'menu', '租户套餐', 'menu:tenant', 'tenant-package', 'view', 41, true),
       ('menu:menu-management', 'menu', '菜单管理', 'menu:tenant', 'menu-management', 'view', 42, true),
       ('menu-management:list', 'action', '查看菜单权限目录', 'menu:menu-management', 'menu-management', 'list', 1, true);
