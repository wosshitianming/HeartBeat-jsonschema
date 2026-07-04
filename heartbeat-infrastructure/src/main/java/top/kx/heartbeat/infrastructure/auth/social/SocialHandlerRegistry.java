package top.kx.heartbeat.infrastructure.auth.social;

import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.auth.SocialLoginHandler;
import top.kx.heartbeat.domain.auth.SocialLoginHandlerRegistry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 社交登录 Handler 注册表：按 provider 编码路由到具体实现。
 */
@Component
public class SocialHandlerRegistry implements SocialLoginHandlerRegistry {

    @Resource
    private List<SocialLoginHandler> handlers;
    private Map<String, SocialLoginHandler> handlerMap;

    @PostConstruct
    public void initializeHandlers() {
        Map<String, SocialLoginHandler> map = new LinkedHashMap<>();
        for (SocialLoginHandler handler : handlers) {
            map.put(handler.provider().toUpperCase(), handler);
        }
        this.handlerMap = map;
    }

    /**
     * 按渠道编码获取 Handler。
     */
    @Override
    public SocialLoginHandler getRequired(String provider) {
        SocialLoginHandler handler = handlerMap.get(provider.toUpperCase());
        if (handler == null) {
            throw new IllegalArgumentException("不支持的登录渠道: " + provider);
        }
        return handler;
    }
}
