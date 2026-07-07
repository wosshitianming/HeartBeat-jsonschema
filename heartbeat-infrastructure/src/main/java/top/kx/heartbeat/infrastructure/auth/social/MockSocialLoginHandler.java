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
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String openId = code.startsWith("mock:") ? code.substring(5) : "demo-open-id";
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, String> profile = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("openId", openId);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("nickname", "演示用户");
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        profile.put("avatar", "");
        // 返回已经完成封装的业务结果。
        return profile;
    }

    private String encode(String value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("UTF-8 编码不可用", ex);
        }
    }
}
