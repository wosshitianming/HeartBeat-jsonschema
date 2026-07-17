-- Quartz JDBC JobStore tables for MySQL 8.0.

CREATE TABLE IF NOT EXISTS `QRTZ_JOB_DETAILS`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `JOB_NAME` VARCHAR
(
    200
) NOT NULL,
    `JOB_GROUP` VARCHAR
(
    200
) NOT NULL,
    `DESCRIPTION` VARCHAR
(
    250
) NULL,
    `JOB_CLASS_NAME` VARCHAR
(
    250
) NOT NULL,
    `IS_DURABLE` VARCHAR
(
    1
) NOT NULL,
    `IS_NONCONCURRENT` VARCHAR
(
    1
) NOT NULL,
    `IS_UPDATE_DATA` VARCHAR
(
    1
) NOT NULL,
    `REQUESTS_RECOVERY` VARCHAR
(
    1
) NOT NULL,
    `JOB_DATA` BLOB NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `JOB_NAME`,
    `JOB_GROUP`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz job details';

CREATE TABLE IF NOT EXISTS `QRTZ_TRIGGERS`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `TRIGGER_NAME` VARCHAR
(
    200
) NOT NULL,
    `TRIGGER_GROUP` VARCHAR
(
    200
) NOT NULL,
    `JOB_NAME` VARCHAR
(
    200
) NOT NULL,
    `JOB_GROUP` VARCHAR
(
    200
) NOT NULL,
    `DESCRIPTION` VARCHAR
(
    250
) NULL,
    `NEXT_FIRE_TIME` BIGINT NULL,
    `PREV_FIRE_TIME` BIGINT NULL,
    `PRIORITY` INT NULL,
    `TRIGGER_STATE` VARCHAR
(
    16
) NOT NULL,
    `TRIGGER_TYPE` VARCHAR
(
    8
) NOT NULL,
    `START_TIME` BIGINT NOT NULL,
    `END_TIME` BIGINT NULL,
    `CALENDAR_NAME` VARCHAR
(
    200
) NULL,
    `MISFIRE_INSTR` SMALLINT NULL,
    `JOB_DATA` BLOB NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
),
    KEY `idx_qrtz_t_j`
(
    `SCHED_NAME`,
    `JOB_NAME`,
    `JOB_GROUP`
),
    KEY `idx_qrtz_t_jg`
(
    `SCHED_NAME`,
    `JOB_GROUP`
),
    KEY `idx_qrtz_t_c`
(
    `SCHED_NAME`,
    `CALENDAR_NAME`
),
    KEY `idx_qrtz_t_g`
(
    `SCHED_NAME`,
    `TRIGGER_GROUP`
),
    KEY `idx_qrtz_t_state`
(
    `SCHED_NAME`,
    `TRIGGER_STATE`
),
    KEY `idx_qrtz_t_n_state`
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`,
    `TRIGGER_STATE`
),
    KEY `idx_qrtz_t_n_g_state`
(
    `SCHED_NAME`,
    `TRIGGER_GROUP`,
    `TRIGGER_STATE`
),
    KEY `idx_qrtz_t_next_fire_time`
(
    `SCHED_NAME`,
    `NEXT_FIRE_TIME`
),
    KEY `idx_qrtz_t_nft_st`
(
    `SCHED_NAME`,
    `TRIGGER_STATE`,
    `NEXT_FIRE_TIME`
),
    KEY `idx_qrtz_t_nft_misfire`
(
    `SCHED_NAME`,
    `MISFIRE_INSTR`,
    `NEXT_FIRE_TIME`
),
    KEY `idx_qrtz_t_nft_st_misfire`
(
    `SCHED_NAME`,
    `MISFIRE_INSTR`,
    `NEXT_FIRE_TIME`,
    `TRIGGER_STATE`
),
    KEY `idx_qrtz_t_nft_st_misfire_grp`
(
    `SCHED_NAME`,
    `MISFIRE_INSTR`,
    `NEXT_FIRE_TIME`,
    `TRIGGER_GROUP`,
    `TRIGGER_STATE`
),
    CONSTRAINT `fk_qrtz_triggers_job_details`
    FOREIGN KEY
(
    `SCHED_NAME`,
    `JOB_NAME`,
    `JOB_GROUP`
)
    REFERENCES `QRTZ_JOB_DETAILS`
(
    `SCHED_NAME`,
    `JOB_NAME`,
    `JOB_GROUP`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz triggers';

CREATE TABLE IF NOT EXISTS `QRTZ_SIMPLE_TRIGGERS`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `TRIGGER_NAME` VARCHAR
(
    200
) NOT NULL,
    `TRIGGER_GROUP` VARCHAR
(
    200
) NOT NULL,
    `REPEAT_COUNT` BIGINT NOT NULL,
    `REPEAT_INTERVAL` BIGINT NOT NULL,
    `TIMES_TRIGGERED` BIGINT NOT NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
),
    CONSTRAINT `fk_qrtz_simple_triggers`
    FOREIGN KEY
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
)
    REFERENCES `QRTZ_TRIGGERS`
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz simple triggers';

CREATE TABLE IF NOT EXISTS `QRTZ_CRON_TRIGGERS`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `TRIGGER_NAME` VARCHAR
(
    200
) NOT NULL,
    `TRIGGER_GROUP` VARCHAR
(
    200
) NOT NULL,
    `CRON_EXPRESSION` VARCHAR
(
    200
) NOT NULL,
    `TIME_ZONE_ID` VARCHAR
(
    80
) NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
),
    CONSTRAINT `fk_qrtz_cron_triggers`
    FOREIGN KEY
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
)
    REFERENCES `QRTZ_TRIGGERS`
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz cron triggers';

CREATE TABLE IF NOT EXISTS `QRTZ_SIMPROP_TRIGGERS`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `TRIGGER_NAME` VARCHAR
(
    200
) NOT NULL,
    `TRIGGER_GROUP` VARCHAR
(
    200
) NOT NULL,
    `STR_PROP_1` VARCHAR
(
    512
) NULL,
    `STR_PROP_2` VARCHAR
(
    512
) NULL,
    `STR_PROP_3` VARCHAR
(
    512
) NULL,
    `INT_PROP_1` INT NULL,
    `INT_PROP_2` INT NULL,
    `LONG_PROP_1` BIGINT NULL,
    `LONG_PROP_2` BIGINT NULL,
    `DEC_PROP_1` DECIMAL
(
    13,
    4
) NULL,
    `DEC_PROP_2` DECIMAL
(
    13,
    4
) NULL,
    `BOOL_PROP_1` VARCHAR
(
    1
) NULL,
    `BOOL_PROP_2` VARCHAR
(
    1
) NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
),
    CONSTRAINT `fk_qrtz_simprop_triggers`
    FOREIGN KEY
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
)
    REFERENCES `QRTZ_TRIGGERS`
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz simple properties triggers';

CREATE TABLE IF NOT EXISTS `QRTZ_BLOB_TRIGGERS`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `TRIGGER_NAME` VARCHAR
(
    200
) NOT NULL,
    `TRIGGER_GROUP` VARCHAR
(
    200
) NOT NULL,
    `BLOB_DATA` BLOB NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
),
    KEY `idx_qrtz_blob_triggers`
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
),
    CONSTRAINT `fk_qrtz_blob_triggers`
    FOREIGN KEY
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
)
    REFERENCES `QRTZ_TRIGGERS`
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz blob triggers';

CREATE TABLE IF NOT EXISTS `QRTZ_CALENDARS`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `CALENDAR_NAME` VARCHAR
(
    200
) NOT NULL,
    `CALENDAR` BLOB NOT NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `CALENDAR_NAME`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz calendars';

CREATE TABLE IF NOT EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `TRIGGER_GROUP` VARCHAR
(
    200
) NOT NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `TRIGGER_GROUP`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz paused trigger groups';

CREATE TABLE IF NOT EXISTS `QRTZ_FIRED_TRIGGERS`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `ENTRY_ID` VARCHAR
(
    95
) NOT NULL,
    `TRIGGER_NAME` VARCHAR
(
    200
) NOT NULL,
    `TRIGGER_GROUP` VARCHAR
(
    200
) NOT NULL,
    `INSTANCE_NAME` VARCHAR
(
    200
) NOT NULL,
    `FIRED_TIME` BIGINT NOT NULL,
    `SCHED_TIME` BIGINT NOT NULL,
    `PRIORITY` INT NOT NULL,
    `STATE` VARCHAR
(
    16
) NOT NULL,
    `JOB_NAME` VARCHAR
(
    200
) NULL,
    `JOB_GROUP` VARCHAR
(
    200
) NULL,
    `IS_NONCONCURRENT` VARCHAR
(
    1
) NULL,
    `REQUESTS_RECOVERY` VARCHAR
(
    1
) NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `ENTRY_ID`
),
    KEY `idx_qrtz_ft_trig_inst_name`
(
    `SCHED_NAME`,
    `INSTANCE_NAME`
),
    KEY `idx_qrtz_ft_inst_job_req_rcvry`
(
    `SCHED_NAME`,
    `INSTANCE_NAME`,
    `REQUESTS_RECOVERY`
),
    KEY `idx_qrtz_ft_j_g`
(
    `SCHED_NAME`,
    `JOB_NAME`,
    `JOB_GROUP`
),
    KEY `idx_qrtz_ft_jg`
(
    `SCHED_NAME`,
    `JOB_GROUP`
),
    KEY `idx_qrtz_ft_t_g`
(
    `SCHED_NAME`,
    `TRIGGER_NAME`,
    `TRIGGER_GROUP`
),
    KEY `idx_qrtz_ft_tg`
(
    `SCHED_NAME`,
    `TRIGGER_GROUP`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz fired triggers';

CREATE TABLE IF NOT EXISTS `QRTZ_SCHEDULER_STATE`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `INSTANCE_NAME` VARCHAR
(
    200
) NOT NULL,
    `LAST_CHECKIN_TIME` BIGINT NOT NULL,
    `CHECKIN_INTERVAL` BIGINT NOT NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `INSTANCE_NAME`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz scheduler state';

CREATE TABLE IF NOT EXISTS `QRTZ_LOCKS`
(
    `SCHED_NAME`
    VARCHAR
(
    120
) NOT NULL,
    `LOCK_NAME` VARCHAR
(
    40
) NOT NULL,
    PRIMARY KEY
(
    `SCHED_NAME`,
    `LOCK_NAME`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = 'Quartz locks';
