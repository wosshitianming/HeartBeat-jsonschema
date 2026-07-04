package top.kx.heartbeat.interfaces.auth.request;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

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
    public Map<String, Object> toMap() {
        // 创建有序字段映射。
        Map<String, Object> payload = new LinkedHashMap<>();
        // 写入登录账号字段。
        payload.put("username", username);
        // 写入登录密码字段。
        payload.put("password", password);
        // 返回登录字段映射。
        return payload;
    }
}
