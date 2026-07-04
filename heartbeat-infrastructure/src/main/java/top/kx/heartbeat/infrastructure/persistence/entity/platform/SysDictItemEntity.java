package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * 系统字典项持久化对象（对应表 sys_dict_item）
 * <p>
 * 一条字典项属于某个 {@code SysDictTypeEntity}。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysDictItemEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 所属字典类型 ID */
    private Long dictTypeId;

    /** 字典项展示标签 */
    private String itemLabel;

    /** 字典项实际值 */
    private String itemValue;

    /** 是否为字典类型的默认值 */
    private Boolean defaultFlag;

    /** 状态 */
    private String status;

    /** 乐观锁版本号 */
    private Integer version;
}
