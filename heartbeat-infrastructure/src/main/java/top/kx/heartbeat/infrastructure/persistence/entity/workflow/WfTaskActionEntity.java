package top.kx.heartbeat.infrastructure.persistence.entity.workflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流任务操作记录持久化实体（对应表 wf_task_action）
 * <p>
 * 记录工作流任务上的每一次操作（通过/驳回/转办/委派/加签/撤回等），用于审计与回放。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class WfTaskActionEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 所属任务 ID */
    private Long taskId;

    /** 操作类型（APPROVE/REJECT/TRANSFER/DELEGATE/ADDSIGN/RECALL/...） */
    private String action;

    /** 操作人用户 ID */
    private Long operatorId;

    /** 操作意见 */
    private String comment;

    /** 创建时间 */
    private LocalDateTime createTime;
}
