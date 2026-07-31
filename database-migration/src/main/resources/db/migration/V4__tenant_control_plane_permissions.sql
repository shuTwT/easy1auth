insert into management_permission (code, type, name, parent_code, resource, action, sort_order, active)
values ('tenant:update', 'action', '更新租户', 'menu:tenant', 'tenant', 'update', 6, true),
       ('tenant:status', 'action', '变更租户状态', 'menu:tenant', 'tenant', 'status', 7, true),
       ('tenant:delete', 'action', '删除租户', 'menu:tenant', 'tenant', 'delete', 8, true);
