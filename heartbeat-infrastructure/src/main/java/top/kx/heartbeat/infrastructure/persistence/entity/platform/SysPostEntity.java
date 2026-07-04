package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * 系统岗位持久化对象（对应表 sys_post）
 * <p>
 * 岗位挂在部门之下，用于描述工作职位。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysPostEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 岗位编码（租户内唯一） */
    private String postCode;

    /** 岗位名称 */
    private String postName;

    /** 岗位类型 */
    private String postType;

    /** 状态 */
    private String status;

    /** 乐观锁版本号 */
    private Integer version;
}
