package top.kx.heartbeat.interfaces.auth.request;

import lombok.Data;
import top.kx.heartbeat.application.platform.request.PlatformLoginRequest;


/**
 * 账号密码登录请求对象。
 *
 * <p>用于承接后台登录时提交的账号与密码。</p>
 */
@Data
public class LoginRequest {

    /**
     * 登录账号。
     */
    private String username;

    /**
     * 登录密码。
     */
    private String password;

    /**
     * 转换为平台认证服务需要的字段映射。
     *
     * @return 登录字段映射
     */
    public PlatformLoginRequest toPlatformRequest() {
        PlatformLoginRequest request = new PlatformLoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
