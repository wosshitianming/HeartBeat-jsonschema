package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统登录日志持久化对象（对应表 sys_login_log）
 * <p>
 * 记录每一次登录尝试及其结果，用于风控与审计。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysLoginLogEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 链路追踪 ID */
    private String traceId;

    /** 登录用户 ID（失败时可空） */
    private Long userId;

    /** 登录用户名 */
    private String username;

    /** 登录方式（PASSWORD/SMS/OAUTH/SSO） */
    private String loginType;

    /** 登录 IP */
    private String loginIp;

    /** 浏览器 User-Agent */
    private String userAgent;

    /** 登录结果（SUCCESS/FAILURE） */
    private String resultStatus;

    /** 失败原因（成功时为空） */
    private String failureReason;

    /** 登录发生时间 */
    private LocalDateTime loggedAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建者 */
    private Long createBy;

    /** 更新者 */
    private Long updateBy;
}
