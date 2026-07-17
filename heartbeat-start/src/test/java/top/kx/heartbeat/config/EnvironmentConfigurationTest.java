package top.kx.heartbeat.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentConfigurationTest {

    private static final List<String> ALL_PROFILES =
            Arrays.asList("local", "dev", "test", "pre", "gray", "prod");
    private static final List<String> MYSQL_PROFILES =
            Arrays.asList("local", "dev", "test", "pre", "gray", "prod");

    @Test
    void commonConfigurationProvidesServerAndMiddlewareDefaults() throws IOException {
        ConfigurableEnvironment environment = load("application.properties");

        assertEquals("7001", environment.getProperty("server.port"));
        assertEquals("dev", environment.getProperty("spring.profiles.active"));
        assertNotNull(environment.getProperty("spring.datasource.druid.max-active"));
        assertEquals("false", environment.getProperty("heartbeat.middleware.redis.enabled"));
        assertEquals("false", environment.getProperty("heartbeat.middleware.rocketmq.enabled"));
        assertEquals("false", environment.getProperty("heartbeat.middleware.kafka.enabled"));
        assertEquals("false", environment.getProperty("heartbeat.middleware.mqtt.enabled"));
        assertEquals("false", environment.getProperty("heartbeat.middleware.quartz.enabled"));
        assertNotNull(environment.getProperty("spring.redis.host"));
        assertNull(environment.getProperty("spring.rabbitmq.host"));
        assertNotNull(environment.getProperty("rocketmq.name-server"));
        assertNotNull(environment.getProperty("rocketmq.producer.group"));
        assertNotNull(environment.getProperty("spring.kafka.bootstrap-servers"));
        assertNotNull(environment.getProperty("heartbeat.middleware.mqtt.host"));
        assertEquals("false", environment.getProperty("management.health.redis.enabled"));
        assertNull(environment.getProperty("management.health.rabbit.enabled"));
        assertNull(environment.getProperty("mybatis-flex.type-aliases-package"));
        assertNull(environment.getProperty("mybatis.type-aliases-package"));
        assertNull(environment.getProperty("spring.data.redis.host"));
    }

    @Test
    void profileConfigurationsExposeEnvironmentSpecificMiddlewareEndpoints() throws IOException {
        for (String profile : ALL_PROFILES) {
            ConfigurableEnvironment environment =
                    load("application.properties", "application-" + profile + ".properties");

            String prefix = profile.toUpperCase();
            assertRawPropertyContains(environment, "heartbeat.middleware.redis.enabled", prefix + "_REDIS_ENABLED");
            assertRawPropertyContains(environment, "heartbeat.middleware.rocketmq.enabled", prefix + "_ROCKETMQ_ENABLED");
            assertRawPropertyContains(environment, "heartbeat.middleware.kafka.enabled", prefix + "_KAFKA_ENABLED");
            assertRawPropertyContains(environment, "heartbeat.middleware.mqtt.enabled", prefix + "_MQTT_ENABLED");
            assertRawPropertyContains(environment, "heartbeat.middleware.quartz.enabled", prefix + "_QUARTZ_ENABLED");
            assertRawPropertyContains(environment, "spring.redis.host", prefix + "_REDIS_HOST");
            assertRawPropertyContains(environment, "rocketmq.name-server", prefix + "_ROCKETMQ_NAME_SERVER");
            assertRawPropertyContains(environment, "spring.kafka.bootstrap-servers",
                    prefix + "_KAFKA_BOOTSTRAP_SERVERS");
            assertRawPropertyContains(environment, "heartbeat.middleware.mqtt.host", prefix + "_MQTT_HOST");
        }
    }

    @Test
    void deployedProfilesUseMysqlWithoutJpaConfiguration() throws IOException {
        for (String profile : MYSQL_PROFILES) {
            ConfigurableEnvironment environment =
                    load("application.properties", "application-" + profile + ".properties");

            assertTrue(environment.getProperty("spring.datasource.druid.url").startsWith("jdbc:mysql:"));
            if (!"local".equals(profile)) {
                assertEquals("false", environment.getProperty("heartbeat.security.dev-auto-login"));
                assertEquals("false", environment.getProperty("heartbeat.security.dev-header-enabled"));
            }
            assertNull(environment.getProperty("spring.jpa.hibernate.ddl-auto"));
            assertNull(environment.getProperty("spring.jpa.open-in-view"));
        }
    }

    @Test
    void localProfileUsesMysqlAndFlyway() throws IOException {
        ConfigurableEnvironment environment = load("application.properties", "application-local.properties");

        assertTrue(environment.getProperty("spring.datasource.druid.url").startsWith("jdbc:mysql:"));
        assertEquals("com.mysql.cj.jdbc.Driver", environment.getProperty("spring.datasource.driver-class-name"));
        assertEquals("never", environment.getProperty("spring.sql.init.mode"));
        assertEquals("true", environment.getProperty("spring.flyway.enabled"));
        assertEquals("true", environment.getProperty("heartbeat.security.dev-auto-login"));
        assertEquals("true", environment.getProperty("heartbeat.security.dev-header-enabled"));
        assertNull(environment.getProperty("spring.jpa.hibernate.ddl-auto"));
    }

    private ConfigurableEnvironment load(String... resources) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();
        for (String resource : resources) {
            List<PropertySource<?>> sources = loader.load(resource, new ClassPathResource(resource));
            for (PropertySource<?> source : sources) {
                environment.getPropertySources().addFirst(source);
            }
        }
        return environment;
    }

    private void assertRawPropertyContains(ConfigurableEnvironment environment, String key, String expectedPart) {
        String rawValue = rawProperty(environment, key);
        assertNotNull(rawValue, "Expected property " + key + " to be present");
        assertTrue(rawValue.contains(expectedPart),
                "Expected raw property " + key + " to contain " + expectedPart + " but was " + rawValue);
    }

    private String rawProperty(ConfigurableEnvironment environment, String key) {
        for (PropertySource<?> source : environment.getPropertySources()) {
            Object value = source.getProperty(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
