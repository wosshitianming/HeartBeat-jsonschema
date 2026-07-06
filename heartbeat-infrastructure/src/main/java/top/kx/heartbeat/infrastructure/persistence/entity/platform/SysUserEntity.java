package top.kx.heartbeat.infrastructure.persistence.entity.platform;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户持久化对象（对应表 sys_user）
 * <p>
 * 描述平台/租户内的登录账号与基础属性，密码以散列方式存储。
 * </p>
 *
 * @author heartbeat-team
 */
@Data
public class SysUserEntity {

    /** 主键 ID */
    private Long id;

    /** 租户 ID（0 表示平台级） */
    private Long tenantId;

    /** 所属部门 ID */
    private Long deptId;

    /** 登录账号（租户内唯一） */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 URL */
    private String avatarUrl;

    /** 登录密码散列值 */
    private String passwordHash;

    /** 密码散列算法（如 BCRYPT/ARGON2） */
    private String passwordAlgo;

    /** 密码最近一次更新时间 */
    private LocalDateTime passwordUpdateTime;

    /** 用户类型（PLATFORM/STAFF/EXTERNAL） */
    private String userType;

    /** 状态（ENABLED/DISABLED/LOCKED） */
    private String status;

    /** 最近一次登录时间 */
    private LocalDateTime lastLoginAt;

    /** 最近一次登录 IP */
    private String lastLoginIp;

    /** 乐观锁版本号 */
    private Integer version;

    /** 逻辑删除标记（0=未删，1=已删） */
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
