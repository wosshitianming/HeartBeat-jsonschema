ALTER TABLE `sys_user`
    CHANGE COLUMN `password_updated_at` `password_update_time` DATETIME(3) NULL COMMENT '密码更新时间';
