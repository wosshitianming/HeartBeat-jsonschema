package top.kx.heartbeat.infrastructure.auth.social;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.auth.SocialLoginHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WechatCpSocialLoginHandler extends AbstractJsonSocialLoginHandler
        implements SocialLoginHandler {

    @Override
    public String provider() {
        return "WECHAT_CP";
    }

    @Override
    public String buildAuthorizeUrl(Map<String, Object> providerConfig, String redirectUri, String state) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("appid", config(providerConfig, "appId"));
        query.put("agentid", config(providerConfig, "agentId"));
        query.put("redirect_uri", redirectUri);
        query.put("state", state);
        return url("https://open.work.weixin.qq.com/wwopen/sso/qrConnect", query);
    }

    @Override
    public Map<String, String> resolveProfile(Map<String, Object> providerConfig, String code) {
        String corpId = config(providerConfig, "appId");
        String secret = config(providerConfig, "appSecret");
        requireValue(corpId, "appId");
        requireValue(secret, "appSecret");

        Map<String, Object> tokenQuery = new LinkedHashMap<>();
        tokenQuery.put("corpid", corpId);
        tokenQuery.put("corpsecret", secret);
        Map<String, Object> token = get(url("https://qyapi.weixin.qq.com/cgi-bin/gettoken", tokenQuery));
        String accessToken = value(token, "access_token");
        requireValue(accessToken, "企业微信 access_token");

        Map<String, Object> identityQuery = new LinkedHashMap<>();
        identityQuery.put("access_token", accessToken);
        identityQuery.put("code", code);
        Map<String, Object> identity = get(url(
                "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo",
                identityQuery
        ));
        String userId = value(identity, "UserId");
        String openId = StringUtils.isEmpty(userId) ? value(identity, "OpenId") : userId;
        requireValue(openId, "企业微信用户标识");

        Map<String, Object> remote = identity;
        if (StringUtils.isNotEmpty(userId)) {
            Map<String, Object> userQuery = new LinkedHashMap<>();
            userQuery.put("access_token", accessToken);
            userQuery.put("userid", userId);
            remote = get(url("https://qyapi.weixin.qq.com/cgi-bin/user/get", userQuery));
        }

        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("openId", openId);
        profile.put("unionId", value(identity, "external_userid"));
        profile.put("nickname", value(remote, "name"));
        profile.put("avatar", value(remote, "avatar"));
        return profile;
    }
}
