package top.kx.heartbeat.infrastructure.auth.social;

import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.auth.SocialLoginHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WechatSocialLoginHandler extends AbstractJsonSocialLoginHandler
        implements SocialLoginHandler {

    @Override
    public String provider() {
        return "WECHAT";
    }

    @Override
    public String buildAuthorizeUrl(Map<String, Object> providerConfig, String redirectUri, String state) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("appid", config(providerConfig, "appId"));
        query.put("redirect_uri", redirectUri);
        query.put("response_type", "code");
        query.put("scope", "snsapi_login");
        query.put("state", state);
        return url("https://open.weixin.qq.com/connect/qrconnect", query) + "#wechat_redirect";
    }

    @Override
    public Map<String, String> resolveProfile(Map<String, Object> providerConfig, String code) {
        String appId = config(providerConfig, "appId");
        String appSecret = config(providerConfig, "appSecret");
        requireValue(appId, "appId");
        requireValue(appSecret, "appSecret");

        Map<String, Object> tokenQuery = new LinkedHashMap<>();
        tokenQuery.put("appid", appId);
        tokenQuery.put("secret", appSecret);
        tokenQuery.put("code", code);
        tokenQuery.put("grant_type", "authorization_code");
        Map<String, Object> token = get(url(
                "https://api.weixin.qq.com/sns/oauth2/access_token",
                tokenQuery
        ));
        String accessToken = value(token, "access_token");
        String openId = value(token, "openid");
        requireValue(accessToken, "微信 access_token");
        requireValue(openId, "微信 openid");

        Map<String, Object> profileQuery = new LinkedHashMap<>();
        profileQuery.put("access_token", accessToken);
        profileQuery.put("openid", openId);
        profileQuery.put("lang", "zh_CN");
        Map<String, Object> remote = get(url("https://api.weixin.qq.com/sns/userinfo", profileQuery));

        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("openId", openId);
        profile.put("unionId", value(remote, "unionid"));
        profile.put("nickname", value(remote, "nickname"));
        profile.put("avatar", value(remote, "headimgurl"));
        return profile;
    }
}
