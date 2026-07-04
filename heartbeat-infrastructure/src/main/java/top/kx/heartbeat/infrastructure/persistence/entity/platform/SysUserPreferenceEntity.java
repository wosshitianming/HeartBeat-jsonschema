package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户偏好设置持久化对象（对应表 sys_user_preference）
 * <p>
 * 按用户存储 KV 形式的个性化设置（语言、主题、通知策略等）。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysUserPreferenceEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 用户 ID */
    private Long userId;

    /** 偏好键 */
    private String preferenceKey;

    /** 偏好值 */
    private String preferenceValue;

    /** 值类型（STRING/NUMBER/BOOLEAN/JSON） */
    private String valueType;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建者 */
    private Long createBy;

    /** 更新者 */
    private Long updateBy;
}
