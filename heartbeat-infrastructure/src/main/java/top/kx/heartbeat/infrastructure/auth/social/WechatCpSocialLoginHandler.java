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
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> query = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("appid", config(providerConfig, "appId"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("agentid", config(providerConfig, "agentId"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("redirect_uri", redirectUri);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        query.put("state", state);
        // 返回已经完成封装的业务结果。
        return url("https://open.work.weixin.qq.com/wwopen/sso/qrConnect", query);
    }

    @Override
    public Map<String, String> resolveProfile(Map<String, Object> providerConfig, String code) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String corpId = config(providerConfig, "appId");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String secret = config(providerConfig, "appSecret");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(corpId, "appId");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(secret, "appSecret");

        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> tokenQuery = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenQuery.put("corpid", corpId);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        tokenQuery.put("corpsecret", secret);
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> token = get(url("https://qyapi.weixin.qq.com/cgi-bin/gettoken", tokenQuery));
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String accessToken = value(token, "access_token");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(accessToken, "企业微信 access_token");

        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> identityQuery = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        identityQuery.put("access_token", accessToken);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        identityQuery.put("code", code);
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> identity = get(url(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                identityQuery
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        ));
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String userId = value(identity, "UserId");
        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
        String openId = StringUtils.isEmpty(userId) ? value(identity, "OpenId") : userId;
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireValue(openId, "企业微信用户标识");

        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Map<String, Object> remote = identity;
        // 根据当前业务条件选择对应处理路径。
        if (StringUtils.isNotEmpty(userId)) {
            // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
            Map<String, Object> userQuery = new LinkedHashMap<>();
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            userQuery.put("access_token", accessToken);
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            userQuery.put("userid", userId);
            // 组装第三方接口请求参数，保证授权调用所需字段完整。
            remote = get(url("https://qyapi.weixin.qq.com/cgi-bin/user/get", userQuery));
        }

        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, String> profile = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("openId", openId);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("unionId", value(identity, "external_userid"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("nickname", value(remote, "name"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("avatar", value(remote, "avatar"));
        // 返回已经完成封装的业务结果。
        return profile;
    }
}
