-- 用户组相互独立。保留所有组及成员/管理员关联，移除层级字段。
-- 原先不同父组下可以同名，平铺后仍保留这些名称，通过组 ID 区分。
drop index if exists uq_user_group_tenant_parent_name;
alter table user_group drop column parent_id;
create index ix_user_group_tenant_name on user_group(tenant_id, name);

-- 清理已移除的用户组树权限及其分配。
drop trigger if exists trg_management_permission_immutable on management_permission;
delete from admin_role_permission where permission_code = 'group:tree';
delete from tenant_package_permission where permission_code = 'group:tree';
delete from management_permission where code = 'group:tree';
create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();
