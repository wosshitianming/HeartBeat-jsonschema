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
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> query = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("appid", config(providerConfig, "appId"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("redirect_uri", redirectUri);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("response_type", "code");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("scope", "snsapi_login");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("state", state);
        // 返回已经完成封装的业务结果。
        return url("https://open.weixin.qq.com/connect/qrconnect", query) + "#wechat_redirect";
    }

    @Override
    public Map<String, String> resolveProfile(Map<String, Object> providerConfig, String code) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String appId = config(providerConfig, "appId");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String appSecret = config(providerConfig, "appSecret");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(appId, "appId");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(appSecret, "appSecret");

        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> tokenQuery = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenQuery.put("appid", appId);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenQuery.put("secret", appSecret);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenQuery.put("code", code);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenQuery.put("grant_type", "authorization_code");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> token = get(url(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "https://api.weixin.qq.com/sns/oauth2/access_token",
                // 组装第三方接口请求参数，保证授权调用所需字段完整。
                tokenQuery
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        ));
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String accessToken = value(token, "access_token");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String openId = value(token, "openid");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(accessToken, "微信 access_token");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(openId, "微信 openid");

        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> profileQuery = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profileQuery.put("access_token", accessToken);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profileQuery.put("openid", openId);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profileQuery.put("lang", "zh_CN");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> remote = get(url("https://api.weixin.qq.com/sns/userinfo", profileQuery));

        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, String> profile = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("openId", openId);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("unionId", value(remote, "unionid"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("nickname", value(remote, "nickname"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("avatar", value(remote, "headimgurl"));
        // 返回已经完成封装的业务结果。
        return profile;
    }
}
