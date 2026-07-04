package top.kx.heartbeat.infrastructure.persistence.entity.tool;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统定时任务执行日志持久化对象（对应表 sys_job_log）
 * <p>
 * 记录每次任务执行的开始/结束时间、结果、消息与耗时。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysJobLogEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 所属任务 ID */
    private Long jobId;

    /** 任务编码（冗余，便于快速排查） */
    private String jobCode;

    /** 调用目标 */
    private String invokeTarget;

    /** 执行结果（SUCCESS/FAILURE） */
    private String resultStatus;

    /** 日志消息（异常堆栈或成功提示） */
    private String message;

    /** 耗时（毫秒） */
    private Long durationMs;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 结束时间 */
    private LocalDateTime finishedAt;

    /** 创建时间 */
    private LocalDateTime createTime;
}
