UPDATE `sys_user`
SET `password_hash`        = '$2a$10$GktqfH9ULvSZcjVlVXMvreLEinbIQ8enqXRlIjYxHlYnNC6JI5/Pi',
    `password_update_time` = CURRENT_TIMESTAMP(3),
    `update_time`          = CURRENT_TIMESTAMP(3)
WHERE `tenant_id` = 1
  AND `username` = 'admin'
  AND `delete_marker` = 0
  AND `password_hash` = '$2a$10$CwTycUXWue0Thq9StjUM0uJ8.7o6iOJIsv4u4tIKu3sZvK7N5Sx9e';
