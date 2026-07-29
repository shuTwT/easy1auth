create table pool_group_admin(
 group_id uuid not null references user_group(id) on delete cascade,
 user_id uuid not null references pool_user(id) on delete cascade,
 primary key(group_id,user_id)
);
