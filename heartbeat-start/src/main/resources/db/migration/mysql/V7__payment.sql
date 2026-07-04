CREATE TABLE `pay_channel`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付渠道ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `name`        VARCHAR(128)    NOT NULL COMMENT '渠道名称',
    `provider`    VARCHAR(32)     NOT NULL COMMENT '支付提供方',
    `app_id`      VARCHAR(128)    NULL COMMENT '应用ID',
    `app_secret`  VARCHAR(512)    NULL COMMENT '应用密钥',
    `status`      VARCHAR(16)     NOT NULL COMMENT '状态',
    `sort_no`     INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    `config_json` JSON            NULL COMMENT '渠道配置',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    KEY `idx_pay_channel_provider` (`tenant_id`, `provider`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付渠道';

CREATE TABLE `pay_order`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付订单ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `order_no`       VARCHAR(64)     NOT NULL COMMENT '订单号',
    `channel_id`     BIGINT UNSIGNED NOT NULL COMMENT '支付渠道ID',
    `subject`        VARCHAR(128)    NOT NULL COMMENT '订单标题',
    `amount`         DECIMAL(20, 4)  NOT NULL COMMENT '订单金额',
    `currency`       VARCHAR(16)     NOT NULL DEFAULT 'CNY' COMMENT '币种',
    `status`         VARCHAR(16)     NOT NULL COMMENT '支付状态',
    `client_ip`      VARCHAR(64)     NULL COMMENT '客户端IP',
    `extra_json`     JSON            NULL COMMENT '扩展信息',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `paid_at`        DATETIME(3)     NULL COMMENT '支付完成时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_order_no` (`tenant_id`, `order_no`),
    KEY `idx_pay_order_status` (`tenant_id`, `status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付订单';

CREATE TABLE `pay_transaction`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付流水ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `order_id`       BIGINT UNSIGNED NOT NULL COMMENT '支付订单ID',
    `transaction_no` VARCHAR(64)     NOT NULL COMMENT '支付流水号',
    `provider`       VARCHAR(32)     NOT NULL COMMENT '支付提供方',
    `amount`         DECIMAL(20, 4)  NOT NULL COMMENT '交易金额',
    `status`         VARCHAR(16)     NOT NULL COMMENT '交易状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_transaction_no` (`tenant_id`, `transaction_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付流水';

CREATE TABLE `pay_refund`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退款单ID',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `order_id`   BIGINT UNSIGNED NOT NULL COMMENT '支付订单ID',
    `refund_no`  VARCHAR(64)     NOT NULL COMMENT '退款单号',
    `amount`     DECIMAL(20, 4)  NOT NULL COMMENT '退款金额',
    `status`     VARCHAR(16)     NOT NULL COMMENT '退款状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_refund_no` (`tenant_id`, `refund_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付退款';

CREATE TABLE `pay_notify_log`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付通知日志ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
    `order_id`        BIGINT UNSIGNED NULL COMMENT '支付订单ID',
    `order_no`        VARCHAR(64)     NULL COMMENT '订单号',
    `provider`        VARCHAR(32)     NOT NULL COMMENT '支付提供方',
    `notify_id`       VARCHAR(96)     NOT NULL COMMENT '通知ID',
    `notify_payload`  JSON            NULL COMMENT '通知报文',
    `signature_valid` VARCHAR(16)     NOT NULL COMMENT '签名校验结果',
    `status`          VARCHAR(16)     NOT NULL COMMENT '处理状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint(20) unsigned NOT NULL COMMENT '创建人ID',
    `update_by` bigint(20) unsigned NOT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_notify_id` (`tenant_id`, `provider`, `notify_id`),
    KEY `idx_pay_notify_order` (`tenant_id`, `order_no`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='支付通知日志';
