package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统部门持久化对象（对应表 sys_dept）
 * <p>
 * 部门以父子结构组织，{@code ancestors} 缓存从根到当前节点的路径。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysDeptEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID（0 表示平台级） */
    private Long tenantId;

    /** 父部门 ID（0 表示顶级） */
    private Long parentId;

    /** 部门编码（租户内唯一） */
    private String deptCode;

    /** 部门名称 */
    private String deptName;

    /** 祖级路径（"1,2,3" 形式） */
    private String ancestors;

    /** 部门层级（1 起步） */
    private Integer deptLevel;

    /** 部门负责人用户 ID */
    private Long leaderUserId;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

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
    private String createBy;

    /** 更新者 */
    private String updateBy;
}
