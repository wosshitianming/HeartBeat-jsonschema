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
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, SocialLoginHandler> map = new LinkedHashMap<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (SocialLoginHandler handler : handlers) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            map.put(handler.provider().toUpperCase(), handler);
        }
        // 计算当前分支的中间结果，供后续判断或组装使用。
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
