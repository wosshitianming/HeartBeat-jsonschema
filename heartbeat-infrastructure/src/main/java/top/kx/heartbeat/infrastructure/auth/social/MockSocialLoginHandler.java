package top.kx.heartbeat.infrastructure.auth.social;

import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.auth.SocialLoginHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MOCK 渠道 Handler：本地联调聚合登录。
 */
@Component
public class MockSocialLoginHandler implements SocialLoginHandler {

    @Override
    public String provider() {
        return "MOCK";
    }

    @Override
    public String buildAuthorizeUrl(Map<String, Object> providerConfig, String redirectUri, String state) {
        return "/login?social=mock&state=" + encode(state);
    }

    @Override
    public Map<String, String> resolveProfile(Map<String, Object> providerConfig, String code) {
        String openId = code.startsWith("mock:") ? code.substring(5) : "demo-open-id";
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("openId", openId);
        profile.put("nickname", "演示用户");
        profile.put("avatar", "");
        return profile;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 编码不可用", ex);
        }
    }
}
