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
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String authorizeUrl = config(providerConfig, "authorizeUrl");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(authorizeUrl, "authorizeUrl");
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> query = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("client_id", config(providerConfig, "appId"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("redirect_uri", redirectUri);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("response_type", "code");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("scope", defaultValue(config(providerConfig, "scopes"), "openid profile"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("state", state);
        // 返回已经完成封装的业务结果。
        return url(authorizeUrl, query);
    }

    @Override
    public Map<String, String> resolveProfile(Map<String, Object> providerConfig, String code) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String tokenUrl = config(providerConfig, "tokenUrl");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String userInfoUrl = config(providerConfig, "userInfoUrl");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(tokenUrl, "tokenUrl");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(userInfoUrl, "userInfoUrl");

        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, String> tokenRequest = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenRequest.put("client_id", config(providerConfig, "appId"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenRequest.put("client_secret", config(providerConfig, "appSecret"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenRequest.put("code", code);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenRequest.put("redirect_uri", config(providerConfig, "redirectUri"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenRequest.put("grant_type", "authorization_code");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> token = postForm(tokenUrl, tokenRequest);
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String accessToken = value(token, "access_token");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(accessToken, "OIDC access_token");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> remote = getBearer(userInfoUrl, accessToken);

        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String openId = value(remote, "sub");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(openId, "OIDC sub");
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, String> profile = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("openId", openId);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("unionId", openId);
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String nickname = value(remote, "name");
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isEmpty(nickname)) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            nickname = value(remote, "preferred_username");
        }
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("nickname", nickname);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("avatar", value(remote, "picture"));
        // 返回已经完成封装的业务结果。
        return profile;
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.isEmpty(value) ? fallback : value;
    }
}
