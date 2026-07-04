package top.kx.heartbeat.infrastructure.auth.social;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.auth.SocialLoginHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OidcSocialLoginHandler extends AbstractJsonSocialLoginHandler
        implements SocialLoginHandler {

    @Override
    public String provider() {
        return "OIDC";
    }

    @Override
    public String buildAuthorizeUrl(Map<String, Object> providerConfig, String redirectUri, String state) {
        String authorizeUrl = config(providerConfig, "authorizeUrl");
        requireValue(authorizeUrl, "authorizeUrl");
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("client_id", config(providerConfig, "appId"));
        query.put("redirect_uri", redirectUri);
        query.put("response_type", "code");
        query.put("scope", defaultValue(config(providerConfig, "scopes"), "openid profile"));
        query.put("state", state);
        return url(authorizeUrl, query);
    }

    @Override
    public Map<String, String> resolveProfile(Map<String, Object> providerConfig, String code) {
        String tokenUrl = config(providerConfig, "tokenUrl");
        String userInfoUrl = config(providerConfig, "userInfoUrl");
        requireValue(tokenUrl, "tokenUrl");
        requireValue(userInfoUrl, "userInfoUrl");

        Map<String, String> tokenRequest = new LinkedHashMap<>();
        tokenRequest.put("client_id", config(providerConfig, "appId"));
        tokenRequest.put("client_secret", config(providerConfig, "appSecret"));
        tokenRequest.put("code", code);
        tokenRequest.put("redirect_uri", config(providerConfig, "redirectUri"));
        tokenRequest.put("grant_type", "authorization_code");
        Map<String, Object> token = postForm(tokenUrl, tokenRequest);
        String accessToken = value(token, "access_token");
        requireValue(accessToken, "OIDC access_token");
        Map<String, Object> remote = getBearer(userInfoUrl, accessToken);

        String openId = value(remote, "sub");
        requireValue(openId, "OIDC sub");
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("openId", openId);
        profile.put("unionId", openId);
        String nickname = value(remote, "name");
        if (StringUtils.isEmpty(nickname)) {
            nickname = value(remote, "preferred_username");
        }
        profile.put("nickname", nickname);
        profile.put("avatar", value(remote, "picture"));
        return profile;
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.isEmpty(value) ? fallback : value;
    }
}
