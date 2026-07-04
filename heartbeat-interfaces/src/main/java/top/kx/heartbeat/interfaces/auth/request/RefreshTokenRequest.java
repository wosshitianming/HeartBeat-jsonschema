package top.kx.heartbeat.interfaces.auth.request;

import lombok.Data;

/**
 * 刷新令牌请求对象。
 *
 * <p>用于承接访问令牌轮换时提交的刷新令牌。</p>
 */
@Data
public class RefreshTokenRequest {

    /**
     * 刷新令牌。
     */
    private String refreshToken;
}
