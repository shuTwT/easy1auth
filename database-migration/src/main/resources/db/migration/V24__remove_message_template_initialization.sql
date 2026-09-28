-- 移除消息模板初始化权限及其角色、套餐分配，保留现有消息模板。
drop trigger if exists trg_management_permission_immutable on management_permission;
delete from admin_role_permission where permission_code = 'message-template:init';
delete from tenant_package_permission where permission_code = 'message-template:init';
delete from management_permission where code = 'message-template:init';
create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();
