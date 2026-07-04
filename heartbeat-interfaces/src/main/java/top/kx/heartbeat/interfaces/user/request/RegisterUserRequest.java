package top.kx.heartbeat.interfaces.user.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 注册用户请求对象。
 *
 * <p>用于接收用户注册接口入参。</p>
 */
@Data
public class RegisterUserRequest {

    /**
     * 用户登录名称。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 64, message = "用户名长度需要在 2 到 64 之间")
    private String username;

    /**
     * 用户邮箱地址。
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不合法")
    private String email;
}
