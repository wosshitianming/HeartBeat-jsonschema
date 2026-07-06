package top.kx.heartbeat.infrastructure.persistence.entity.pay;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付渠道持久化实体。
 *
 * <p>映射 pay_channel 表。</p>
 */
@Data
public class PayChannelEntity {

    /**
     * 支付渠道主键。
     */
    private Long id;

    /**
     * 租户主键。
     */
    private Long tenantId;

    /**
     * 支付渠道名称。
     */
    private String name;

    /**
     * 支付服务提供方。
     */
    private String provider;

    /**
     * 支付应用标识。
     */
    private String appId;

    /**
     * 支付应用密钥。
     */
    private String appSecret;

    /**
     * 支付渠道状态。
     */
    private String status;

    /**
     * 排序号。
     */
    private Integer sortNo;

    /**
     * 渠道配置 JSON。
     */
    private String configJson;

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
    private String createBy;

    /**
     * 更新人主键。
     */
    private String updateBy;
}
