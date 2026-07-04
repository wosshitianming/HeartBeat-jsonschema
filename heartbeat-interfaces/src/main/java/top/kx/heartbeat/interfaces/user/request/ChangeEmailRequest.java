package top.kx.heartbeat.interfaces.user.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 修改邮箱请求对象。
 *
 * <p>用于接收用户修改邮箱接口入参。</p>
 */
@Data
public class ChangeEmailRequest {

    /**
     * 用户新邮箱地址。
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不合法")
    private String email;
}
