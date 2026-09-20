-- 现有 enterprise_identity_source 实际是通讯录同步配置，改名为同步身份源。
-- 企业身份源（登录授权）由 social_identity_source 中的 feishu_web 提供。

drop trigger if exists trg_management_permission_immutable on management_permission;

insert into management_permission(code, type, name, parent_code, resource, action, sort_order, active)
values ('menu:sync-identity-source', 'menu', '同步身份源', 'directory:identity-source-management', 'sync-identity-source', 'view', 32, true);

update admin_role_permission
set permission_code = 'menu:sync-identity-source'
where permission_code = 'menu:enterprise-identity-source';
update tenant_package_permission
set permission_code = 'menu:sync-identity-source'
where permission_code = 'menu:enterprise-identity-source';
update management_permission
set parent_code = 'menu:sync-identity-source', name = replace(name, '企业身份源', '同步身份源')
where parent_code = 'menu:enterprise-identity-source';
delete from management_permission where code = 'menu:enterprise-identity-source';

-- 为真正的企业登录身份源增加独立菜单。其页面复用 social_identity_source 的
-- 权限动作，但只展示 feishu_web（飞书网页授权）。
insert into management_permission(code, type, name, parent_code, resource, action, sort_order, active)
values ('menu:enterprise-identity-source', 'menu', '企业身份源', 'directory:identity-source-management', 'enterprise-identity-source', 'view', 31, true);
insert into tenant_package_permission(package_id, permission_code)
select p.id, 'menu:enterprise-identity-source'
from tenant_package p
where exists (
    select 1 from tenant_package_permission tpp
    where tpp.package_id = p.id and tpp.permission_code = 'menu:sync-identity-source'
)
on conflict do nothing;

insert into admin_role_permission(role_id, permission_code)
select arp.role_id, 'menu:enterprise-identity-source'
from admin_role_permission arp
where arp.permission_code = 'menu:sync-identity-source'
on conflict do nothing;

create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();
