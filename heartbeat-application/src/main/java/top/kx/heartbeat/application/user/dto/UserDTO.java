package top.kx.heartbeat.application.user.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * 用户数据传输对象。
 *
 * <p>用于应用层向接口层传递用户数据。</p>
 */
@Value
@Builder
public class UserDTO {

    /**
     * 用户主键标识。
     */
    Long id;

    /**
     * 用户登录名称。
     */
    String username;

    /**
     * 用户邮箱地址。
     */
    String email;

    /**
     * 用户状态编码。
     */
    String status;

    /**
     * 用户创建时间。
     */
    Instant createTime;

    /**
     * 用户更新时间。
     */
    Instant updateTime;
}
