package top.kx.heartbeat.infrastructure.persistence.entity.tool;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统定时任务持久化对象（对应表 sys_job）
 * <p>
 * 描述由 Quartz 调度的可执行任务，包括目标、cron 表达式与并发策略。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysJobEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 任务编码（租户内唯一） */
    private String jobCode;

    /** 任务名称 */
    private String jobName;

    /** 任务分组 */
    private String jobGroup;

    /** 调用目标（如 beanName.method(params)） */
    private String invokeTarget;

    /** Cron 表达式 */
    private String cronExpression;

    /** 错失触发策略（FIRE_AND_PROCEED/WITH_ALL_LOST_INSTANCES/...） */
    private String misfirePolicy;

    /** 是否允许并发执行 */
    private Boolean concurrent;

    /** 状态（ENABLED/DISABLED/PAUSED） */
    private String status;

    /** 乐观锁版本号 */
    private Integer version;

    /** 逻辑删除标记 */
    private Long deleteMarker;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建者 */
    private String createBy;

    /** 更新者 */
    private String updateBy;
}
