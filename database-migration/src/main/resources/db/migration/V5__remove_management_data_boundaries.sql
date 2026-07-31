drop trigger if exists trg_management_permission_immutable on management_permission;

delete from admin_role_permission
where permission_code in ('data:platform:all', 'data:tenant:all');

delete from tenant_package_permission
where permission_code in ('data:platform:all', 'data:tenant:all');

delete from management_permission
where code in ('data:platform:all', 'data:tenant:all');

alter table management_permission
    drop constraint ck_management_permission_type;

alter table management_permission
    add constraint ck_management_permission_type
        check (type in ('directory', 'menu', 'action'));

create trigger trg_management_permission_immutable
    before update or delete
    on management_permission
    for each row execute function reject_management_permission_change();
