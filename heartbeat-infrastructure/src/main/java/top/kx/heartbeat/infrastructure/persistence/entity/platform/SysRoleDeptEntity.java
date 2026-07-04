package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * 角色-部门关联持久化对象（对应表 sys_role_dept）
 * <p>
 * 自定义数据权限（{@code dataScope=CUSTOM}）下，限定角色可访问的部门集合。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysRoleDeptEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 角色 ID */
    private Long roleId;

    /** 部门 ID */
    private Long deptId;
}
