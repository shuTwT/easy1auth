-- Keep social identity source selection in the login-style draft until it is published.
alter table login_style
    add column draft_social_providers jsonb not null default '[]'::jsonb;

update login_style
set draft_social_providers = social_providers;
