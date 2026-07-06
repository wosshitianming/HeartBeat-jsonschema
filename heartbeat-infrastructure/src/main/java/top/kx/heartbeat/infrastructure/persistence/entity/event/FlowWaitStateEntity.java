package top.kx.heartbeat.infrastructure.persistence.entity.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程等待状态持久化实体。
 *
 * <p>映射 flow_wait_state 表。</p>
 */
@Data
public class FlowWaitStateEntity {

    /**
     * 流程等待状态主键。
     */
    private Long id;

    /**
     * 租户主键。
     */
    private Long tenantId;

    /**
     * 流程运行主键。
     */
    private Long runId;

    /**
     * 等待节点标识。
     */
    private String nodeId;

    /**
     * 事件关联键。
     */
    private String correlationKey;

    /**
     * 等待状态编码。
     */
    private String status;

    /**
     * 等待载荷 JSON。
     */
    private String payloadJson;

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
