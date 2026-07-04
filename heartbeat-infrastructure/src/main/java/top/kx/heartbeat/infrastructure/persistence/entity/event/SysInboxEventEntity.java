package top.kx.heartbeat.infrastructure.persistence.entity.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 可靠消息 Inbox 持久化实体。
 *
 * <p>映射 sys_inbox_event 表。</p>
 */
@Data
public class SysInboxEventEntity {

    /**
     * Inbox 自增主键。
     */
    private Long id;

    /**
     * 租户主键。
     */
    private Long tenantId;

    /**
     * 消费者编码。
     */
    private String consumerCode;

    /**
     * 事件唯一标识。
     */
    private String eventId;

    /**
     * 消费状态编码。
     */
    private String status;

    /**
     * 处理完成时间。
     */
    private LocalDateTime processedAt;
}
