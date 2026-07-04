package top.kx.heartbeat.infrastructure.persistence.entity.pay;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单持久化实体。
 *
 * <p>映射 pay_order 表。</p>
 */
@Data
public class PayOrderEntity {

    /**
     * 支付订单主键。
     */
    private Long id;

    /**
     * 租户主键。
     */
    private Long tenantId;

    /**
     * 支付订单号。
     */
    private String orderNo;

    /**
     * 支付渠道主键。
     */
    private Long channelId;

    /**
     * 支付订单标题。
     */
    private String subject;

    /**
     * 支付金额。
     */
    private BigDecimal amount;

    /**
     * 支付币种。
     */
    private String currency;

    /**
     * 支付订单状态。
     */
    private String status;

    /**
     * 客户端 IP 地址。
     */
    private String clientIp;

    /**
     * 扩展信息 JSON。
     */
    private String extraJson;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    /**
     * 创建人主键。
     */
    private Long createBy;

    /**
     * 更新人主键。
     */
    private Long updateBy;

    /**
     * 支付完成时间。
     */
    private LocalDateTime paidAt;
}
