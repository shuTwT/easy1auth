-- V15: 登录样式可视化编辑器草稿/发布快照。

alter table login_style
    add column draft_config jsonb not null default '{}'::jsonb,
    add column published_config jsonb not null default '{}'::jsonb,
    add column draft_updated_at timestamptz not null default now(),
    add column published_at timestamptz;

alter table legal_document_setting
    add column draft_terms_of_service text,
    add column draft_privacy_policy text,
    add column published_terms_of_service text,
    add column published_privacy_policy text;

update legal_document_setting
set draft_terms_of_service = terms_of_service,
    draft_privacy_policy = privacy_policy,
    published_terms_of_service = terms_of_service,
    published_privacy_policy = privacy_policy;

update login_style
set published_config = jsonb_build_object(
        'schemaVersion', 1,
        'global', jsonb_build_object(
            'title', title,
            'subtitle', subtitle,
            'logoUrl', logo,
            'logoDarkUrl', logo_dark,
            'background', jsonb_build_object(
                'mode', case when background_image is null then 'solid' else 'image' end,
                'color', background_color,
                'imageUrl', background_image,
                'overlayColor', null,
                'overlayOpacity', 0
            ),
            'primaryColor', primary_color,
            'language', 'zh-CN',
            'card', jsonb_build_object('width', 480, 'radius', 16, 'shadow', true, 'position', 'center'),
            'customCss', custom_css
        ),
        'standard', jsonb_build_object(
            'enabled', true,
            'methods', coalesce((
                select jsonb_agg(case when value = 'oidc' then 'social' else value end)
                from jsonb_array_elements_text(login_methods)
            ), '["password"]'::jsonb),
            'registrationEnabled', registration_enabled,
            'termsRequired', false
        ),
        'qr', jsonb_build_object(
            'enabled', false,
            'title', '扫码登录',
            'subtitle', '使用手机扫码继续',
            'iconUrl', null
        )
    ),
    draft_updated_at = updated_at,
    published_at = updated_at;

update login_style
set draft_config = published_config;
