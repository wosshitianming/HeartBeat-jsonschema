package top.kx.heartbeat.interfaces.user.response;

import lombok.Builder;
import lombok.Data;
import top.kx.heartbeat.application.user.dto.UserDTO;

import java.time.Instant;

/**
 * 用户响应对象。
 *
 * <p>用于向接口调用方返回用户基础信息。</p>
 */
@Data
@Builder
public class UserResponse {

    /**
     * 用户主键标识。
     */
    private Long id;

    /**
     * 用户登录名称。
     */
    private String username;

    /**
     * 用户邮箱地址。
     */
    private String email;

    /**
     * 用户状态编码。
     */
    private String status;

    /**
     * 用户创建时间。
     */
    private Instant createTime;

    /**
     * 用户更新时间。
     */
    private Instant updateTime;

    /**
     * 将应用层用户 DTO 转换为接口响应对象。
     *
     * @param dto 应用层用户 DTO
     * @return 用户响应对象
     */
    public static UserResponse from(UserDTO dto) {
        // 创建用户响应构建器。
        UserResponseBuilder builder = UserResponse.builder();
        // 写入用户主键标识。
        builder.id(dto.getId());
        // 写入用户登录名称。
        builder.username(dto.getUsername());
        // 写入用户邮箱地址。
        builder.email(dto.getEmail());
        // 写入用户状态编码。
        builder.status(dto.getStatus());
        // 写入用户创建时间。
        builder.createTime(dto.getCreateTime());
        // 写入用户更新时间。
        builder.updateTime(dto.getUpdateTime());
        // 构建用户响应对象。
        return builder.build();
    }
}
