package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统权限点持久化对象（对应表 sys_permission）
 * <p>
 * 细粒度权限点：API、按钮或路由都可以挂载到本表。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysPermissionEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 权限编码（租户内唯一） */
    private String permissionCode;

    /** 权限名称 */
    private String permissionName;

    /** 权限类型（MENU/BUTTON/API） */
    private String permissionType;

    /** 资源类型（API/UI/...） */
    private String resourceType;

    /** 资源路径（API path 或 UI 标识） */
    private String resourcePath;

    /** HTTP 方法（API 类权限专用） */
    private String httpMethod;

    /** 描述 */
    private String description;

    /** 状态 */
    private String status;

    /** 排序号 */
    private Integer sortNo;

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
