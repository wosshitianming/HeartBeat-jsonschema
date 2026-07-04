package top.kx.heartbeat.infrastructure.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 社交登录 HTTP 客户端配置。
 *
 * <p>统一注册社交登录回调用的 RestTemplate，避免业务处理器自行创建 HTTP 客户端。</p>
 */
@Configuration
public class SocialHttpConfig {

    /**
     * 注册社交登录 RestTemplate。
     *
     * @param builder RestTemplate 构建器。
     * @return 社交登录 RestTemplate。
     */
    @Bean
    public RestTemplate socialRestTemplate(RestTemplateBuilder builder) {
        // 使用 Spring 托管的构建器创建社交登录 HTTP 客户端。
        return builder
                // 设置社交平台连接超时时间。
                .setConnectTimeout(Duration.ofSeconds(5))
                // 设置社交平台读取超时时间。
                .setReadTimeout(Duration.ofSeconds(10))
                // 构建 RestTemplate 实例。
                .build();
    }
}
