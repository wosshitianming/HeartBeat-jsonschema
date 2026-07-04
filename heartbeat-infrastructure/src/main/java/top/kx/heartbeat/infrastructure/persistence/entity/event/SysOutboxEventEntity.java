package top.kx.heartbeat.infrastructure.persistence.entity.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 可靠消息 Outbox 持久化实体。
 *
 * <p>映射 sys_outbox_event 表。</p>
 */
@Data
public class SysOutboxEventEntity {

    /**
     * Outbox 自增主键。
     */
    private Long id;

    /**
     * 租户主键。
     */
    private Long tenantId;

    /**
     * 事件唯一标识。
     */
    private String eventId;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 聚合类型。
     */
    private String aggregateType;

    /**
     * 聚合标识。
     */
    private String aggregateId;

    /**
     * 事件载荷 JSON。
     */
    private String payloadJson;

    /**
     * 事件状态编码。
     */
    private String status;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 发布时间。
     */
    private LocalDateTime publishedAt;
}
