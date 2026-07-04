package top.kx.heartbeat.infrastructure.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中间件自动装配开关过滤器。
 *
 * <p>通过枚举维护自动装配类与配置开关的映射，避免中间件未启用时触发无效 Bean 创建。</p>
 */
public class MiddlewareAutoConfigurationImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    /**
     * Redis 中间件启用开关。
     */
    private static final String REDIS_ENABLED = "heartbeat.middleware.redis.enabled";

    /**
     * RocketMQ 中间件启用开关。
     */
    private static final String ROCKETMQ_ENABLED = "heartbeat.middleware.rocketmq.enabled";

    /**
     * Kafka 中间件启用开关。
     */
    private static final String KAFKA_ENABLED = "heartbeat.middleware.kafka.enabled";

    /**
     * Quartz 中间件启用开关。
     */
    private static final String QUARTZ_ENABLED = "heartbeat.middleware.quartz.enabled";

    /**
     * 自动装配类到配置开关的索引。
     */
    private static final Map<String, MiddlewareAutoConfigurationSwitch> AUTO_CONFIGURATION_SWITCHES =
            Arrays.stream(MiddlewareAutoConfigurationSwitch.values())
                    .collect(Collectors.toMap(MiddlewareAutoConfigurationSwitch::getAutoConfigurationClass, Function.identity()));

    /**
     * Spring 环境配置读取器。
     */
    private Environment environment;

    /**
     * 注入 Spring 环境配置读取器。
     *
     * @param environment Spring 环境配置读取器。
     */
    @Override
    public void setEnvironment(Environment environment) {
        // 保存环境对象，供自动装配过滤阶段读取中间件开关。
        this.environment = environment;
    }

    /**
     * 判断每个自动装配类是否允许导入。
     *
     * @param autoConfigurationClasses 自动装配类名数组。
     * @param autoConfigurationMetadata 自动装配元数据。
     * @return 自动装配类匹配结果数组。
     */
    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        // 创建与自动装配类数组等长的匹配结果数组。
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        // 默认允许 Spring Boot 原生自动装配继续生效。
        Arrays.fill(matches, true);
        // 逐个检查自动装配类是否被项目中间件开关托管。
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            // 按自动装配类名查找对应的中间件开关枚举。
            MiddlewareAutoConfigurationSwitch middlewareSwitch = AUTO_CONFIGURATION_SWITCHES.get(autoConfigurationClasses[i]);
            // 中间件未启用时阻止对应自动装配类进入 Spring 容器。
            if (middlewareSwitch != null && !isEnabled(middlewareSwitch)) {
                // 标记当前自动装配类不匹配。
                matches[i] = false;
            }
        }
        // 返回 Spring Boot 自动装配导入过滤结果。
        return matches;
    }

    /**
     * 判断中间件开关是否启用。
     *
     * @param middlewareSwitch 中间件自动装配开关枚举。
     * @return 是否启用。
     */
    private boolean isEnabled(MiddlewareAutoConfigurationSwitch middlewareSwitch) {
        // 环境尚未注入时按未启用处理，避免提前加载可选中间件。
        return environment != null && environment.getProperty(middlewareSwitch.getSwitchProperty(), Boolean.class, false);
    }

    /**
     * 中间件自动装配开关枚举。
     */
    private enum MiddlewareAutoConfigurationSwitch {

        /**
         * Redis 基础自动装配。
         */
        REDIS_AUTO_CONFIGURATION(
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                REDIS_ENABLED
        ),

        /**
         * Redis Reactive 自动装配。
         */
        REDIS_REACTIVE_AUTO_CONFIGURATION(
                "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
                REDIS_ENABLED
        ),

        /**
         * Redis 仓储自动装配。
         */
        REDIS_REPOSITORIES_AUTO_CONFIGURATION(
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                REDIS_ENABLED
        ),

        /**
         * Redis 健康检查自动装配。
         */
        REDIS_HEALTH_CONTRIBUTOR_AUTO_CONFIGURATION(
                "org.springframework.boot.actuate.autoconfigure.redis.RedisHealthContributorAutoConfiguration",
                REDIS_ENABLED
        ),

        /**
         * Redis Reactive 健康检查自动装配。
         */
        REDIS_REACTIVE_HEALTH_CONTRIBUTOR_AUTO_CONFIGURATION(
                "org.springframework.boot.actuate.autoconfigure.redis.RedisReactiveHealthContributorAutoConfiguration",
                REDIS_ENABLED
        ),

        /**
         * Redis Lettuce 指标自动装配。
         */
        LETTUCE_METRICS_AUTO_CONFIGURATION(
                "org.springframework.boot.actuate.autoconfigure.metrics.redis.LettuceMetricsAutoConfiguration",
                REDIS_ENABLED
        ),

        /**
         * RocketMQ 自动装配。
         */
        ROCKETMQ_AUTO_CONFIGURATION(
                "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration",
                ROCKETMQ_ENABLED
        ),

        /**
         * Kafka 自动装配。
         */
        KAFKA_AUTO_CONFIGURATION(
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                KAFKA_ENABLED
        ),

        /**
         * Kafka 指标自动装配。
         */
        KAFKA_METRICS_AUTO_CONFIGURATION(
                "org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration",
                KAFKA_ENABLED
        ),

        /**
         * Quartz 自动装配。
         */
        QUARTZ_AUTO_CONFIGURATION(
                "org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration",
                QUARTZ_ENABLED
        ),

        /**
         * Quartz 端点自动装配。
         */
        QUARTZ_ENDPOINT_AUTO_CONFIGURATION(
                "org.springframework.boot.actuate.autoconfigure.quartz.QuartzEndpointAutoConfiguration",
                QUARTZ_ENABLED
        );

        /**
         * 自动装配类名。
         */
        private final String autoConfigurationClass;

        /**
         * 自动装配启用开关配置项。
         */
        private final String switchProperty;

        /**
         * 绑定自动装配类与中间件开关。
         *
         * @param autoConfigurationClass 自动装配类名。
         * @param switchProperty 自动装配启用开关配置项。
         */
        MiddlewareAutoConfigurationSwitch(String autoConfigurationClass, String switchProperty) {
            // 保存自动装配类名。
            this.autoConfigurationClass = autoConfigurationClass;
            // 保存自动装配启用开关配置项。
            this.switchProperty = switchProperty;
        }

        /**
         * 获取自动装配类名。
         *
         * @return 自动装配类名。
         */
        private String getAutoConfigurationClass() {
            // 返回当前枚举绑定的自动装配类名。
            return autoConfigurationClass;
        }

        /**
         * 获取自动装配启用开关配置项。
         *
         * @return 自动装配启用开关配置项。
         */
        private String getSwitchProperty() {
            // 返回当前枚举绑定的启用开关配置项。
            return switchProperty;
        }
    }
}
