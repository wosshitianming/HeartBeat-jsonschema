package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置项持久化对象（对应表 sys_config）
 * <p>
 * 通用 KV 配置，{@code encrypted=true} 时 {@code configValue} 密文存储。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysConfigEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 配置键（租户内唯一） */
    private String configKey;

    /** 配置名称 */
    private String configName;

    /** 配置值 */
    private String configValue;

    /** 值类型（STRING/NUMBER/BOOLEAN/JSON） */
    private String valueType;

    /** 是否加密 */
    private Boolean encrypted;

    /** 配置分组 */
    private String configGroup;

    /** 描述 */
    private String description;

    /** 状态 */
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
    private Long createBy;

    /** 更新者 */
    private Long updateBy;
}
