package top.kx.heartbeat.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import top.kx.heartbeat.infrastructure.config.MiddlewareAutoConfigurationImportFilter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MiddlewareAutoConfigurationImportFilterTest {

    @Test
    void rejectsMiddlewareAutoConfigurationsWhenSwitchesAreDisabled() {
        MiddlewareAutoConfigurationImportFilter filter = new MiddlewareAutoConfigurationImportFilter();
        filter.setEnvironment(new MockEnvironment()
                .withProperty("heartbeat.middleware.redis.enabled", "false")
                .withProperty("heartbeat.middleware.rocketmq.enabled", "false")
                .withProperty("heartbeat.middleware.kafka.enabled", "false")
                .withProperty("heartbeat.middleware.quartz.enabled", "false"));

        boolean[] matches = filter.match(new String[] {
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration",
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration",
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
        }, null);

        assertArrayEquals(new boolean[] {false, false, false, false, true}, matches);
    }

    @Test
    void keepsMiddlewareAutoConfigurationsWhenSwitchesAreEnabled() {
        MiddlewareAutoConfigurationImportFilter filter = new MiddlewareAutoConfigurationImportFilter();
        filter.setEnvironment(new MockEnvironment()
                .withProperty("heartbeat.middleware.redis.enabled", "true")
                .withProperty("heartbeat.middleware.rocketmq.enabled", "true")
                .withProperty("heartbeat.middleware.kafka.enabled", "true")
                .withProperty("heartbeat.middleware.quartz.enabled", "true"));

        boolean[] matches = filter.match(new String[] {
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration",
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration"
        }, null);

        assertArrayEquals(new boolean[] {true, true, true, true}, matches);
    }
}
