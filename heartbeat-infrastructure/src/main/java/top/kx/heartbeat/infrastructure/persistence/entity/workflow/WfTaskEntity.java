package top.kx.heartbeat.infrastructure.persistence.entity.workflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流任务持久化实体。
 *
 * <p>映射 wf_task 表。</p>
 */
@Data
public class WfTaskEntity {

    /**
     * 工作流任务主键。
     */
    private Long id;

    /**
     * 租户主键。
     */
    private Long tenantId;

    /**
     * 流程实例主键。
     */
    private Long instanceId;

    /**
     * 任务名称。
     */
    private String name;

    /**
     * 任务处理人主键。
     */
    private Long assigneeId;

    /**
     * 任务状态。
     */
    private String status;

    /**
     * 任务处理意见。
     */
    private String comment;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 完成时间。
     */
    private LocalDateTime completedAt;
}
