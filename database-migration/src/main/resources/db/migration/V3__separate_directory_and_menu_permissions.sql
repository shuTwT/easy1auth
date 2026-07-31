alter table management_permission drop constraint ck_management_permission_type;
alter table management_permission add constraint ck_management_permission_type
    check (type in ('directory', 'menu', 'action', 'data'));

drop trigger trg_management_permission_immutable on management_permission;

update management_permission
set type = 'directory'
where code = 'menu:user-management';

insert into management_permission (code, type, name, parent_code, resource, action, sort_order, active)
values ('directory:platform-management', 'directory', '平台管理', null, 'platform-management', 'view', 40, true);

update management_permission
set parent_code = 'directory:platform-management'
where code in ('menu:tenant', 'menu:tenant-package', 'menu:menu-management', 'menu:admin-user');

update management_permission
set parent_code = 'menu:tenant-package'
where type = 'action' and resource = 'tenant-package';

create trigger trg_management_permission_immutable
    before update or delete
    on management_permission
    for each row execute function reject_management_permission_change();
