package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统角色持久化对象（对应表 sys_role）
 * <p>
 * 角色是权限授予的载体，{@code dataScope} 字段决定数据可见范围。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysRoleEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID（0 表示平台级） */
    private Long tenantId;

    /** 角色编码（租户内唯一） */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 角色类型（PLATFORM/TENANT/CUSTOM） */
    private String roleType;

    /** 数据权限范围（ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM） */
    private String dataScope;

    /** 描述 */
    private String description;

    /** 排序号 */
    private Integer sortNo;

    /** 状态（ENABLED/DISABLED） */
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
    private String createBy;

    /** 更新者 */
    private String updateBy;
}
