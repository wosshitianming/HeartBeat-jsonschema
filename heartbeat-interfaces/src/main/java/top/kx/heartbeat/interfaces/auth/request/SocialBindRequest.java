package top.kx.heartbeat.interfaces.auth.request;

import lombok.Data;

/**
 * 第三方账号绑定请求对象。
 *
 * <p>用于承接第三方账号绑定本地账号时提交的票据与账号密码。</p>
 */
@Data
public class SocialBindRequest {

    /**
     * 第三方回调绑定票据。
     */
    private String bindTicket;

    /**
     * 本地登录账号。
     */
    private String username;

    /**
     * 本地登录密码。
     */
    private String password;
}
