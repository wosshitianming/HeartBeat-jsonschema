package top.kx.heartbeat.domain.auth;

import java.util.Map;

/**
 * 聚合第三方登录处理器端口（参考 Pig LoginHandler 策略）。
 */
public interface SocialLoginHandler {

    /**
     * 渠道编码，如 WECHAT、DINGTALK、MOCK。
     */
    String provider();

    /**
     * 拼接第三方授权页 URL。
     */
    String buildAuthorizeUrl(Map<String, Object> providerConfig, String redirectUri, String state);

    /**
     * 用授权码换取第三方用户画像。
     */
    Map<String, String> resolveProfile(Map<String, Object> providerConfig, String code);
}
