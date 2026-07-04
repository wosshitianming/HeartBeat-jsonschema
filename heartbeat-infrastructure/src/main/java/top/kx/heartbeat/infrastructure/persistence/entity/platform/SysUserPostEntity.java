package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

/**
 * 用户-岗位关联持久化对象（对应表 sys_user_post）
 *
 * @author heartbeat-team
 */
@Data
public class SysUserPostEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 用户 ID */
    private Long userId;

    /** 岗位 ID */
    private Long postId;

    /** 是否主岗 */
    private Boolean primaryPost;
}
