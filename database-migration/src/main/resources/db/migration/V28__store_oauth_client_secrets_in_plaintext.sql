-- OAuth client secrets are intentionally retrievable by the admin application detail API.
-- Existing BCrypt values cannot be converted back to plaintext, so they are invalidated.
alter table oauth_application
    rename column client_secret_hash to client_secret;

update oauth_application
set client_secret = null
where client_secret is not null;
