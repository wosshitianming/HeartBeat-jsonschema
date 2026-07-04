package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单-权限关联持久化对象（对应表 sys_menu_permission）
 * <p>
 * 一个菜单可关联多个权限点（用于按钮级控制）。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysMenuPermissionEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 菜单 ID */
    private Long menuId;

    /** 权限点 ID */
    private Long permissionId;

    /** 创建人 ID */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
