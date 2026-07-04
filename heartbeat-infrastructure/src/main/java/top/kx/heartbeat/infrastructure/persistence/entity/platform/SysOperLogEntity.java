package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统操作日志持久化对象（对应表 sys_oper_log）
 * <p>
 * 记录每一次管理端 API 调用的请求与结果概要，用于审计。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysOperLogEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 链路追踪 ID */
    private String traceId;

    /** 业务模块编码 */
    private String moduleCode;

    /** 操作类型（CREATE/UPDATE/DELETE/QUERY/...） */
    private String operationType;

    /** 操作名称（如"新增用户"） */
    private String operationName;

    /** 操作人用户 ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** HTTP 方法 */
    private String requestMethod;

    /** 请求路径 */
    private String requestPath;

    /** 请求 IP */
    private String requestIp;

    /** 浏览器 User-Agent */
    private String userAgent;

    /** 请求参数（脱敏后） */
    private String requestParams;

    /** 响应摘要 */
    private String responseSummary;

    /** 调用结果（SUCCESS/FAILURE） */
    private String resultStatus;

    /** 错误码（失败时） */
    private String errorCode;

    /** 错误信息（失败时） */
    private String errorMessage;

    /** 调用耗时（毫秒） */
    private Long durationMs;

    /** 操作发生时间 */
    private LocalDateTime operatedAt;
}
