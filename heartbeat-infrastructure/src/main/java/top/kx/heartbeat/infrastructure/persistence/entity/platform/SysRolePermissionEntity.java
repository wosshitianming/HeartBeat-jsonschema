package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-权限关联持久化对象（对应表 sys_role_permission）
 *
 * @author heartbeat-team
 */
@Data
public class SysRolePermissionEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 角色 ID */
    private Long roleId;

    /** 权限点 ID */
    private Long permissionId;

    /** 创建人 ID */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
