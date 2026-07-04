package top.kx.heartbeat.infrastructure.auth.social;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.auth.SocialLoginHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DingTalkSocialLoginHandler extends AbstractJsonSocialLoginHandler
        implements SocialLoginHandler {

    @Override
    public String provider() {
        return "DINGTALK";
    }

    @Override
    public String buildAuthorizeUrl(Map<String, Object> providerConfig, String redirectUri, String state) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("client_id", config(providerConfig, "appId"));
        query.put("redirect_uri", redirectUri);
        query.put("response_type", "code");
        query.put("scope", "openid");
        query.put("state", state);
        query.put("prompt", "consent");
        return url("https://login.dingtalk.com/oauth2/auth", query);
    }

    @Override
    public Map<String, String> resolveProfile(Map<String, Object> providerConfig, String code) {
        Map<String, Object> tokenRequest = new LinkedHashMap<>();
        tokenRequest.put("clientId", config(providerConfig, "appId"));
        tokenRequest.put("clientSecret", config(providerConfig, "appSecret"));
        tokenRequest.put("code", code);
        tokenRequest.put("grantType", "authorization_code");
        Map<String, Object> token = postJson(
                "https://api.dingtalk.com/v1.0/oauth2/userAccessToken",
                tokenRequest
        );
        String accessToken = value(token, "accessToken");
        requireValue(accessToken, "钉钉 accessToken");
        Map<String, Object> remote = getBearer(
                "https://api.dingtalk.com/v1.0/contact/users/me",
                accessToken
        );

        String openId = value(remote, "openId");
        if (StringUtils.isEmpty(openId)) {
            openId = value(remote, "unionId");
        }
        requireValue(openId, "钉钉用户标识");
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("openId", openId);
        profile.put("unionId", value(remote, "unionId"));
        profile.put("nickname", value(remote, "nick"));
        profile.put("avatar", value(remote, "avatarUrl"));
        return profile;
    }
}
