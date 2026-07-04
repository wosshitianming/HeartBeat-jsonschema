package top.kx.heartbeat.interfaces.auth.request;

import lombok.Data;

/**
 * 第三方登录回调请求对象。
 *
 * <p>用于承接前端主动提交的 OAuth 授权码与状态值。</p>
 */
@Data
public class SocialCallbackRequest {

    /**
     * OAuth 授权码。
     */
    private String code;

    /**
     * 防重放状态值。
     */
    private String state;
}
