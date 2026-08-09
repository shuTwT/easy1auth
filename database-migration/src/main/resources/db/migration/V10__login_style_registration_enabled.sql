alter table login_style add column registration_enabled boolean not null default true;

comment on column login_style.registration_enabled is '是否允许终端用户自助注册 pool_user';
