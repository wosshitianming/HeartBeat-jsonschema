package top.kx.heartbeat.config;

import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import top.kx.heartbeat.domain.tool.QuartzJobScheduler;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "heartbeat.middleware.redis.enabled=false",
        "heartbeat.middleware.rocketmq.enabled=false",
        "heartbeat.middleware.kafka.enabled=false",
        "heartbeat.middleware.mqtt.enabled=false",
        "heartbeat.middleware.quartz.enabled=false"
})
@ActiveProfiles("local")
class MiddlewareToggleContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private QuartzJobScheduler quartzJobScheduler;

    @Test
    void startsWithoutDisabledMiddlewareClientsOrSchedulers() {
        assertEquals(0, applicationContext.getBeansOfType(RedisConnectionFactory.class).size());
        assertEquals(0, applicationContext.getBeansOfType(KafkaTemplate.class).size());
        assertEquals(0, applicationContext.getBeansOfType(Scheduler.class).size());
        assertFalse(applicationContext.containsBean("rocketMQTemplate"));
        assertFalse(applicationContext.containsBean("quartzJobStartupRunner"));
        assertFalse(Arrays.stream(applicationContext.getBeanDefinitionNames())
                .anyMatch(name -> applicationContext.getType(name) != null
                        && applicationContext.getType(name).getName().startsWith("org.springframework.amqp")));
    }

    @Test
    void disabledQuartzSchedulerReportsThatQuartzIsNotEnabled() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> quartzJobScheduler.runNow("demo", "DEFAULT"));

        assertEquals("Quartz 未启用", exception.getMessage());
    }
}
