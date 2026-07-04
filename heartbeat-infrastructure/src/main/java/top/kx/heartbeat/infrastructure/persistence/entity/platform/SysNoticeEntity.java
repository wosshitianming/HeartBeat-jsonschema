package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * 系统通知公告持久化对象（对应表 sys_notice）
 * <p>
 * 支持按 {@code publishScope}（ALL/DEPT/ROLE/CUSTOM）指定接收范围。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysNoticeEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 通知标题 */
    private String noticeTitle;

    /** 通知类型（NOTICE/ANNOUNCEMENT/WARNING） */
    private String noticeType;

    /** 通知内容（可含富文本/HTML） */
    private String noticeContent;

    /** 发布范围（ALL/DEPT/ROLE/CUSTOM） */
    private String publishScope;

    /** 发布状态（DRAFT/PUBLISHED/REVOKED） */
    private String publishStatus;

    /** 乐观锁版本号 */
    private Integer version;
}
