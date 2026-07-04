package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-角色关联持久化对象（对应表 sys_user_role）
 * <p>
 * 多对多关系表：用户与角色之间的绑定关系。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysUserRoleEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 用户 ID */
    private Long userId;

    /** 角色 ID */
    private Long roleId;

    /** 创建人 ID */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
