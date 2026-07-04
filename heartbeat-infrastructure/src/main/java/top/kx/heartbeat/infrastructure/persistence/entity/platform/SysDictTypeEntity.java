package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * 系统字典类型持久化对象（对应表 sys_dict_type）
 * <p>
 * 字典是若干字典项（{@code SysDictItemEntity}）的容器。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysDictTypeEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 字典编码（租户内唯一） */
    private String dictCode;

    /** 字典名称 */
    private String dictName;

    /** 状态 */
    private String status;

    /** 乐观锁版本号 */
    private Integer version;
}
