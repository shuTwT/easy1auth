-- 用户组仅维护成员，移除组管理员关联。
drop table if exists pool_group_admin;

-- 清理组管理员操作权限及其角色、套餐分配。
drop trigger if exists trg_management_permission_immutable on management_permission;
delete from admin_role_permission
where permission_code in ('group:admin:add', 'group:admin:remove');
delete from tenant_package_permission
where permission_code in ('group:admin:add', 'group:admin:remove');
delete from management_permission
where code in ('group:admin:add', 'group:admin:remove');
create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();
