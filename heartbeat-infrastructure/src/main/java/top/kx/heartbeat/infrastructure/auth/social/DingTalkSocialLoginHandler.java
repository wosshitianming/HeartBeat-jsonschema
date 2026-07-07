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
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> query = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("client_id", config(providerConfig, "appId"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("redirect_uri", redirectUri);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("response_type", "code");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("scope", "openid");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("state", state);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("prompt", "consent");
        // 返回已经完成封装的业务结果。
        return url("https://login.dingtalk.com/oauth2/auth", query);
    }

    @Override
    public Map<String, String> resolveProfile(Map<String, Object> providerConfig, String code) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> tokenRequest = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenRequest.put("clientId", config(providerConfig, "appId"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenRequest.put("clientSecret", config(providerConfig, "appSecret"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenRequest.put("code", code);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenRequest.put("grantType", "authorization_code");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> token = postJson(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "https://api.dingtalk.com/v1.0/oauth2/userAccessToken",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                tokenRequest
        );
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String accessToken = value(token, "accessToken");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(accessToken, "钉钉 accessToken");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> remote = getBearer(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "https://api.dingtalk.com/v1.0/contact/users/me",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                accessToken
        );

        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String openId = value(remote, "openId");
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isEmpty(openId)) {
            // 提取第三方登录返回字段，后续用于绑定或创建本地用户。
            openId = value(remote, "unionId");
        }
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(openId, "钉钉用户标识");
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, String> profile = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("openId", openId);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("unionId", value(remote, "unionId"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("nickname", value(remote, "nick"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("avatar", value(remote, "avatarUrl"));
        // 返回已经完成封装的业务结果。
        return profile;
    }
}
