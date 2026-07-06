package top.kx.heartbeat.infrastructure.persistence.entity.workflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流流程实例持久化实体。
 *
 * <p>映射 wf_process_instance 表。</p>
 */
@Data
public class WfProcessInstanceEntity {

    /**
     * 流程实例主键。
     */
    private Long id;

    /**
     * 租户主键。
     */
    private Long tenantId;

    /**
     * 流程定义主键。
     */
    private Long definitionId;

    /**
     * 业务标识。
     */
    private String businessKey;

    /**
     * 流程实例标题。
     */
    private String title;

    /**
     * 发起人主键。
     */
    private Long initiatorId;

    /**
     * 流程实例状态。
     */
    private String status;

    /**
     * 当前任务主键。
     */
    private Long currentTaskId;

    /**
     * 流程实例载荷 JSON。
     */
    private String payload;

    /**
     * 启动时间。
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间。
     */
    private LocalDateTime endedAt;

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
