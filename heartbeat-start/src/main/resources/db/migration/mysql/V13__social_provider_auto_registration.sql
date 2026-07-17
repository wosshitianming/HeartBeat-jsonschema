ALTER TABLE `auth_social_provider`
    ADD COLUMN `auto_register` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether successful social login may create a local user' AFTER `enabled`;
