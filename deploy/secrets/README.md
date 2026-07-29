# Secret files

Create the following files locally with mode `0600`. They are ignored by Git:

- `db_password.txt` (at least 16 UTF-8 bytes)
- `admin_jwt_secret.txt` (at least 32 UTF-8 bytes)
- `oauth2_key_encryption_secret.txt` (at least 32 UTF-8 bytes)
- `security_data_encryption_secret.txt` (at least 32 UTF-8 bytes)
- `smtp_username.txt`
- `smtp_password.txt` (at least 12 UTF-8 bytes)
- `bootstrap_admin_username.txt`
- `bootstrap_admin_email.txt`
- `bootstrap_admin_password.txt` (8-128 characters with upper-case, lower-case, and numeric characters)

Do not copy example values into production. The production profile rejects missing, short,
and common placeholder secret values. Bootstrap credentials are mounted only into the
explicit `bootstrap-admin` profile task.

SMTP uses port 465 with implicit TLS by default. Set `REGISTRATION_FROM_ADDRESS` in
`deploy/.env.production` to an address permitted by the authenticated SMTP account.
