-- V14: 完全移除管理面板品牌定制，并将法律条款收敛为独立能力。

drop trigger if exists trg_management_permission_immutable on management_permission;

insert into management_permission(code, type, name, parent_code, resource, action, sort_order, active)
values
    ('legal-document:read', 'action', '查看法律文档', 'menu:brand-login-style', 'legal-document', 'read', 20, true),
    ('legal-document:update', 'action', '更新法律文档', 'menu:brand-login-style', 'legal-document', 'update', 21, true);

insert into admin_role_permission(role_id, permission_code, created_at)
select role_id,
       case permission_code
           when 'brand:read' then 'legal-document:read'
           when 'brand:update' then 'legal-document:update'
       end,
       created_at
from admin_role_permission
where permission_code in ('brand:read', 'brand:update')
on conflict (role_id, permission_code) do nothing;

insert into tenant_package_permission(package_id, permission_code, created_at)
select package_id,
       case permission_code
           when 'brand:read' then 'legal-document:read'
           when 'brand:update' then 'legal-document:update'
       end,
       created_at
from tenant_package_permission
where permission_code in ('brand:read', 'brand:update')
on conflict (package_id, permission_code) do nothing;

delete from admin_role_permission
where permission_code in ('brand:read', 'brand:update', 'menu:admin-panel', 'menu:brand-settings');
delete from tenant_package_permission
where permission_code in ('brand:read', 'brand:update', 'menu:admin-panel', 'menu:brand-settings');

delete from management_permission
where code in ('brand:read', 'brand:update', 'menu:admin-panel', 'menu:brand-settings');

create trigger trg_management_permission_immutable
    before update or delete on management_permission
    for each row execute function reject_management_permission_change();

alter table brand_setting rename to legal_document_setting;
alter table legal_document_setting add column terms_of_service text;
alter table legal_document_setting add column privacy_policy text;

update legal_document_setting
set terms_of_service = settings #>> '{legalDocuments,termsOfService}',
    privacy_policy = settings #>> '{legalDocuments,privacyPolicy}';

alter table legal_document_setting
    drop column settings,
    drop column company_name,
    drop column logo,
    drop column favicon,
    drop column primary_color,
    drop column secondary_color,
    drop column support_email,
    drop column copyright_text;

alter table legal_document_setting
    rename constraint brand_setting_pkey to legal_document_setting_pkey;
alter table legal_document_setting
    rename constraint brand_setting_tenant_id_fkey to legal_document_setting_tenant_id_fkey;
alter table legal_document_setting
    rename constraint uq_brand_setting_tenant to uq_legal_document_setting_tenant;
