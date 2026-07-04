package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统菜单持久化对象（对应表 sys_menu）
 * <p>
 * 描述前端路由与权限点，{@code permissionMode} 决定权限校验粒度。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysMenuEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID（0 表示平台级） */
    private Long tenantId;

    /** 父菜单 ID（0 表示顶级） */
    private Long parentId;

    /** 菜单编码（租户内唯一） */
    private String menuCode;

    /** 菜单名称 */
    private String menuName;

    /** 菜单类型（DIR/MENU/BUTTON） */
    private String menuType;

    /** 路由地址 */
    private String routePath;

    /** 前端组件路径 */
    private String componentPath;

    /** 重定向地址 */
    private String redirectPath;

    /** 菜单图标 */
    private String icon;

    /** 是否显示 */
    private Boolean visible;

    /** 是否缓存 */
    private Boolean keepAlive;

    /** 外链地址（可空） */
    private String externalLink;

    /** 权限模式（PATH/BUTTON） */
    private String permissionMode;

    /** 排序号 */
    private Integer sortNo;

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
