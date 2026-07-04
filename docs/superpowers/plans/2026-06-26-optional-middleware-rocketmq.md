# Optional Middleware RocketMQ Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace RabbitMQ with RocketMQ and make Redis, RocketMQ, Kafka, MQTT, and Quartz opt-in so the application starts without those external services by default.

**Architecture:** A single early Spring Boot auto-configuration import filter prevents disabled middleware clients, health contributors, metrics, listeners, and schedulers from being created. Profile files keep environment-specific endpoints and switches, while the common configuration keeps all switches defaulted to `false`. Quartz gets a disabled adapter so application services can still wire when scheduling is off.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Maven multi-module, Apache RocketMQ Spring Boot Starter 2.2.0, JUnit 5, Spring Boot Test.

---

## File structure

- Modify `pom.xml`: add the managed RocketMQ Spring Boot Starter version.
- Modify `heartbeat-infrastructure/pom.xml`: remove `spring-boot-starter-amqp`; add `rocketmq-spring-boot-starter`.
- Create `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/config/MiddlewareAutoConfigurationImportFilter.java`: filters Redis/RocketMQ/Kafka/Quartz auto-configurations when their switches are disabled.
- Create `heartbeat-infrastructure/src/main/resources/META-INF/spring.factories`: registers the auto-configuration import filter.
- Modify `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/config/QuartzConfig.java`: only creates Quartz integration beans when Quartz is enabled.
- Modify `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/quartz/QuartzSchedulerManager.java`: only creates the real scheduler port adapter when Quartz is enabled.
- Create `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/quartz/DisabledQuartzJobScheduler.java`: provides a disabled Quartz adapter that fails with `Quartz 未启用`.
- Modify `heartbeat-application/src/main/java/top/kx/heartbeat/application/tool/QuartzJobStartupRunner.java`: only refreshes Quartz jobs at startup when Quartz is enabled.
- Create `heartbeat-application/src/main/java/top/kx/heartbeat/application/tool/QuartzEnabledCondition.java`: lightweight Spring condition using `spring-context`, avoiding a new application-layer dependency on `spring-boot-autoconfigure`.
- Modify `heartbeat-start/src/main/resources/application.properties`: add default-off middleware switches, replace RabbitMQ properties with RocketMQ properties, and make health/listener/scheduler defaults follow switches.
- Modify `heartbeat-start/src/main/resources/application-local.properties`, `application-dev.properties`, `application-test.properties`, `application-pre.properties`, `application-gray.properties`, `application-prod.properties`: add profile-specific endpoint placeholders and remove RabbitMQ overrides.
- Modify `k8s/configmap.yaml` and `k8s/secret.yaml`: remove RabbitMQ variables; add default-off switches and RocketMQ endpoint/secrets.
- Modify `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnvironmentConfigurationTest.java`: assert default switches, RabbitMQ removal, RocketMQ presence, and profile endpoint placeholders.
- Create `heartbeat-start/src/test/java/top/kx/heartbeat/config/MiddlewareAutoConfigurationImportFilterTest.java`: unit-test switch-to-auto-configuration filtering.
- Create `heartbeat-start/src/test/java/top/kx/heartbeat/config/MiddlewareToggleContextTest.java`: verify local startup with all middleware disabled creates no external clients or Quartz scheduler.
- Modify `README.md`: document default-off middleware behavior, RocketMQ replacement, and profile-specific endpoint variables.

---

### Task 1: Write failing configuration contract tests

**Files:**
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnvironmentConfigurationTest.java`
- Create: `heartbeat-start/src/test/java/top/kx/heartbeat/config/MiddlewareAutoConfigurationImportFilterTest.java`
- Create: `heartbeat-start/src/test/java/top/kx/heartbeat/config/MiddlewareToggleContextTest.java`

- [ ] **Step 1: Update common property assertions**

In `EnvironmentConfigurationTest.commonConfigurationProvidesServerAndMiddlewareDefaults`, replace the RabbitMQ assertion with explicit default-off switch and RocketMQ assertions:

```java
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
assertEquals("${REDIS_HEALTH_ENABLED:${heartbeat.middleware.redis.enabled:false}}",
        environment.getProperty("management.health.redis.enabled"));
assertNull(environment.getProperty("management.health.rabbit.enabled"));
```

- [ ] **Step 2: Add profile endpoint assertions**

Add this test method to `EnvironmentConfigurationTest`:

```java
@Test
void profileConfigurationsExposeEnvironmentSpecificMiddlewareEndpoints() throws IOException {
    for (String profile : Arrays.asList("local", "dev", "test", "pre", "gray", "prod")) {
        ConfigurableEnvironment environment =
                load("application.properties", "application-" + profile + ".properties");

        String prefix = profile.toUpperCase();
        assertTrue(environment.getProperty("spring.redis.host").contains(prefix + "_REDIS_HOST")
                || "local".equals(profile));
        assertTrue(environment.getProperty("rocketmq.name-server").contains(prefix + "_ROCKETMQ_NAME_SERVER")
                || "local".equals(profile));
        assertTrue(environment.getProperty("spring.kafka.bootstrap-servers")
                .contains(prefix + "_KAFKA_BOOTSTRAP_SERVERS") || "local".equals(profile));
        assertTrue(environment.getProperty("heartbeat.middleware.mqtt.host").contains(prefix + "_MQTT_HOST")
                || "local".equals(profile));
    }
}
```

- [ ] **Step 3: Add auto-configuration import filter unit tests**

Create `MiddlewareAutoConfigurationImportFilterTest.java`:

```java
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
```

- [ ] **Step 4: Add a Spring context test for disabled middleware**

Create `MiddlewareToggleContextTest.java`:

```java
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
```

- [ ] **Step 5: Run the new tests and confirm they fail**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnvironmentConfigurationTest,MiddlewareAutoConfigurationImportFilterTest,MiddlewareToggleContextTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation or assertion failure because the filter and disabled Quartz adapter do not exist yet, RabbitMQ properties still exist, and RocketMQ properties are not configured.

---

### Task 2: Replace RabbitMQ dependency with RocketMQ

**Files:**
- Modify: `pom.xml`
- Modify: `heartbeat-infrastructure/pom.xml`

- [ ] **Step 1: Add the RocketMQ Spring version property**

Add this property to the root `pom.xml` `<properties>` section:

```xml
<rocketmq-spring.version>2.2.0</rocketmq-spring.version>
```

- [ ] **Step 2: Manage the RocketMQ starter dependency**

Add this dependency to root `pom.xml` `<dependencyManagement><dependencies>`:

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>${rocketmq-spring.version}</version>
</dependency>
```

- [ ] **Step 3: Replace the infrastructure dependency**

In `heartbeat-infrastructure/pom.xml`, delete:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

Add:

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>
```

- [ ] **Step 4: Run the dependency tree check**

Run:

```powershell
mvn --% -pl heartbeat-infrastructure dependency:tree -Dincludes=org.springframework.boot:spring-boot-starter-amqp,org.apache.rocketmq:rocketmq-spring-boot-starter
```

Expected: RocketMQ starter appears and AMQP starter does not appear.

---

### Task 3: Implement early middleware auto-configuration filtering

**Files:**
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/config/MiddlewareAutoConfigurationImportFilter.java`
- Create: `heartbeat-infrastructure/src/main/resources/META-INF/spring.factories`

- [ ] **Step 1: Create the filter class**

Create `MiddlewareAutoConfigurationImportFilter.java`:

```java
package top.kx.heartbeat.infrastructure.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MiddlewareAutoConfigurationImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    private static final String REDIS_ENABLED = "heartbeat.middleware.redis.enabled";
    private static final String ROCKETMQ_ENABLED = "heartbeat.middleware.rocketmq.enabled";
    private static final String KAFKA_ENABLED = "heartbeat.middleware.kafka.enabled";
    private static final String QUARTZ_ENABLED = "heartbeat.middleware.quartz.enabled";

    private static final Map<String, String> AUTO_CONFIGURATION_SWITCHES = new HashMap<>();

    static {
        register("org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration", REDIS_ENABLED);
        register("org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration", REDIS_ENABLED);
        register("org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration", REDIS_ENABLED);
        register("org.springframework.boot.actuate.autoconfigure.redis.RedisHealthContributorAutoConfiguration", REDIS_ENABLED);
        register("org.springframework.boot.actuate.autoconfigure.redis.RedisReactiveHealthContributorAutoConfiguration", REDIS_ENABLED);
        register("org.springframework.boot.actuate.autoconfigure.metrics.redis.LettuceMetricsAutoConfiguration", REDIS_ENABLED);
        register("org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration", ROCKETMQ_ENABLED);
        register("org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration", KAFKA_ENABLED);
        register("org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration", KAFKA_ENABLED);
        register("org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration", QUARTZ_ENABLED);
        register("org.springframework.boot.actuate.autoconfigure.quartz.QuartzEndpointAutoConfiguration", QUARTZ_ENABLED);
    }

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        Arrays.fill(matches, true);
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            String switchProperty = AUTO_CONFIGURATION_SWITCHES.get(autoConfigurationClasses[i]);
            if (switchProperty != null && !isEnabled(switchProperty)) {
                matches[i] = false;
            }
        }
        return matches;
    }

    private static void register(String autoConfigurationClass, String switchProperty) {
        AUTO_CONFIGURATION_SWITCHES.put(autoConfigurationClass, switchProperty);
    }

    private boolean isEnabled(String switchProperty) {
        return environment != null && environment.getProperty(switchProperty, Boolean.class, false);
    }
}
```

- [ ] **Step 2: Register the filter**

Create `heartbeat-infrastructure/src/main/resources/META-INF/spring.factories`:

```properties
org.springframework.boot.autoconfigure.AutoConfigurationImportFilter=\
top.kx.heartbeat.infrastructure.config.MiddlewareAutoConfigurationImportFilter
```

- [ ] **Step 3: Run the filter unit tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=MiddlewareAutoConfigurationImportFilterTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `MiddlewareAutoConfigurationImportFilterTest` passes.

---

### Task 4: Make Quartz safe when disabled

**Files:**
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/config/QuartzConfig.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/quartz/QuartzSchedulerManager.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/quartz/DisabledQuartzJobScheduler.java`
- Modify: `heartbeat-application/src/main/java/top/kx/heartbeat/application/tool/QuartzJobStartupRunner.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/tool/QuartzEnabledCondition.java`

- [ ] **Step 1: Add the application-layer Quartz condition**

Create `QuartzEnabledCondition.java`:

```java
package top.kx.heartbeat.application.tool;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

class QuartzEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return context.getEnvironment()
                .getProperty("heartbeat.middleware.quartz.enabled", Boolean.class, false);
    }
}
```

- [ ] **Step 2: Guard the startup runner**

Modify `QuartzJobStartupRunner.java`:

```java
@Component
@Conditional(QuartzEnabledCondition.class)
public class QuartzJobStartupRunner implements ApplicationRunner {
```

Add this import:

```java
import org.springframework.context.annotation.Conditional;
```

- [ ] **Step 3: Guard Quartz infrastructure beans**

Add this import to `QuartzConfig.java` and `QuartzSchedulerManager.java`:

```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
```

Add this annotation above each class:

```java
@ConditionalOnProperty(prefix = "heartbeat.middleware.quartz", name = "enabled", havingValue = "true")
```

- [ ] **Step 4: Add the disabled scheduler adapter**

Create `DisabledQuartzJobScheduler.java`:

```java
package top.kx.heartbeat.infrastructure.quartz;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.tool.QuartzJobScheduler;
import top.kx.heartbeat.domain.tool.model.ScheduledJob;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "heartbeat.middleware.quartz", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledQuartzJobScheduler implements QuartzJobScheduler {

    private static final String MESSAGE = "Quartz 未启用";

    @Override
    public void refreshJobs(List<ScheduledJob> jobs) {
        throw disabled();
    }

    @Override
    public void runNow(String jobId, String jobGroup) {
        throw disabled();
    }

    @Override
    public void pause(String jobId, String jobGroup) {
        throw disabled();
    }

    @Override
    public void resume(String jobId, String jobGroup) {
        throw disabled();
    }

    private IllegalStateException disabled() {
        return new IllegalStateException(MESSAGE);
    }
}
```

- [ ] **Step 5: Run the context test**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=MiddlewareToggleContextTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: context starts with `local` profile and disabled middleware, and the test passes.

---

### Task 5: Update common and profile configuration

**Files:**
- Modify: `heartbeat-start/src/main/resources/application.properties`
- Modify: `heartbeat-start/src/main/resources/application-local.properties`
- Modify: `heartbeat-start/src/main/resources/application-dev.properties`
- Modify: `heartbeat-start/src/main/resources/application-test.properties`
- Modify: `heartbeat-start/src/main/resources/application-pre.properties`
- Modify: `heartbeat-start/src/main/resources/application-gray.properties`
- Modify: `heartbeat-start/src/main/resources/application-prod.properties`

- [ ] **Step 1: Add default-off common switches**

Insert this block before the Redis properties in `application.properties`:

```properties
heartbeat.middleware.redis.enabled=${REDIS_ENABLED:false}
heartbeat.middleware.rocketmq.enabled=${ROCKETMQ_ENABLED:false}
heartbeat.middleware.kafka.enabled=${KAFKA_ENABLED:false}
heartbeat.middleware.mqtt.enabled=${MQTT_ENABLED:false}
heartbeat.middleware.quartz.enabled=${QUARTZ_ENABLED:false}
```

- [ ] **Step 2: Replace RabbitMQ with RocketMQ in common properties**

Delete all `spring.rabbitmq.*` entries from `application.properties`.

Add:

```properties
rocketmq.name-server=${ROCKETMQ_NAME_SERVER:127.0.0.1:9876}
rocketmq.access-channel=${ROCKETMQ_ACCESS_CHANNEL:LOCAL}
rocketmq.producer.group=${ROCKETMQ_PRODUCER_GROUP:heartbeat-${spring.profiles.active}}
rocketmq.producer.access-key=${ROCKETMQ_ACCESS_KEY:}
rocketmq.producer.secret-key=${ROCKETMQ_SECRET_KEY:}
rocketmq.producer.send-message-timeout=${ROCKETMQ_SEND_MESSAGE_TIMEOUT:3000}
rocketmq.producer.retry-times-when-send-failed=${ROCKETMQ_RETRY_TIMES_WHEN_SEND_FAILED:2}
rocketmq.producer.retry-times-when-send-async-failed=${ROCKETMQ_RETRY_TIMES_WHEN_SEND_ASYNC_FAILED:2}
rocketmq.producer.max-message-size=${ROCKETMQ_MAX_MESSAGE_SIZE:4194304}
rocketmq.producer.retry-next-server=${ROCKETMQ_RETRY_NEXT_SERVER:false}
rocketmq.consumer.group=${ROCKETMQ_CONSUMER_GROUP:heartbeat-${spring.profiles.active}}
rocketmq.consumer.topic=${ROCKETMQ_CONSUMER_TOPIC:heartbeat-events}
rocketmq.consumer.access-key=${ROCKETMQ_ACCESS_KEY:}
rocketmq.consumer.secret-key=${ROCKETMQ_SECRET_KEY:}
rocketmq.consumer.message-model=${ROCKETMQ_CONSUMER_MESSAGE_MODEL:CLUSTERING}
rocketmq.consumer.selector-type=${ROCKETMQ_CONSUMER_SELECTOR_TYPE:TAG}
rocketmq.consumer.selector-expression=${ROCKETMQ_CONSUMER_SELECTOR_EXPRESSION:*}
rocketmq.consumer.pull-batch-size=${ROCKETMQ_CONSUMER_PULL_BATCH_SIZE:10}
```

- [ ] **Step 3: Make listener, scheduler, and health defaults follow switches**

Set:

```properties
spring.kafka.listener.auto-startup=${KAFKA_LISTENER_AUTO_STARTUP:${heartbeat.middleware.kafka.enabled:false}}
spring.quartz.auto-startup=${QUARTZ_AUTO_STARTUP:${heartbeat.middleware.quartz.enabled:false}}
management.health.redis.enabled=${REDIS_HEALTH_ENABLED:${heartbeat.middleware.redis.enabled:false}}
management.endpoint.quartz.enabled=${QUARTZ_ENDPOINT_ENABLED:${heartbeat.middleware.quartz.enabled:false}}
```

Delete:

```properties
management.health.rabbit.enabled=${RABBITMQ_HEALTH_ENABLED:true}
```

- [ ] **Step 4: Add profile-specific switch and endpoint placeholders**

For each profile file, add the profile-specific form below, replacing `DEV` and `dev` with `LOCAL/local`, `TEST/test`, `PRE/pre`, `GRAY/gray`, and `PROD/prod` as appropriate:

```properties
heartbeat.middleware.redis.enabled=${DEV_REDIS_ENABLED:${REDIS_ENABLED:false}}
heartbeat.middleware.rocketmq.enabled=${DEV_ROCKETMQ_ENABLED:${ROCKETMQ_ENABLED:false}}
heartbeat.middleware.kafka.enabled=${DEV_KAFKA_ENABLED:${KAFKA_ENABLED:false}}
heartbeat.middleware.mqtt.enabled=${DEV_MQTT_ENABLED:${MQTT_ENABLED:false}}
heartbeat.middleware.quartz.enabled=${DEV_QUARTZ_ENABLED:${QUARTZ_ENABLED:false}}

spring.redis.host=${DEV_REDIS_HOST:${REDIS_HOST:127.0.0.1}}
spring.redis.port=${DEV_REDIS_PORT:${REDIS_PORT:6379}}
rocketmq.name-server=${DEV_ROCKETMQ_NAME_SERVER:${ROCKETMQ_NAME_SERVER:127.0.0.1:9876}}
rocketmq.producer.group=${DEV_ROCKETMQ_PRODUCER_GROUP:${ROCKETMQ_PRODUCER_GROUP:heartbeat-dev}}
rocketmq.consumer.group=${DEV_ROCKETMQ_CONSUMER_GROUP:${ROCKETMQ_CONSUMER_GROUP:heartbeat-dev}}
spring.kafka.bootstrap-servers=${DEV_KAFKA_BOOTSTRAP_SERVERS:${KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}}
heartbeat.middleware.mqtt.host=${DEV_MQTT_HOST:${MQTT_HOST:127.0.0.1}}
heartbeat.middleware.mqtt.port=${DEV_MQTT_PORT:${MQTT_PORT:1883}}
```

For `application-dev.properties`, use `192.168.204.12` as the endpoint fallback to match the existing dev database host:

```properties
spring.redis.host=${DEV_REDIS_HOST:${REDIS_HOST:192.168.204.12}}
rocketmq.name-server=${DEV_ROCKETMQ_NAME_SERVER:${ROCKETMQ_NAME_SERVER:192.168.204.12:9876}}
spring.kafka.bootstrap-servers=${DEV_KAFKA_BOOTSTRAP_SERVERS:${KAFKA_BOOTSTRAP_SERVERS:192.168.204.12:9092}}
heartbeat.middleware.mqtt.host=${DEV_MQTT_HOST:${MQTT_HOST:192.168.204.12}}
```

- [ ] **Step 5: Remove profile RabbitMQ overrides**

Delete these entries wherever present:

```properties
spring.rabbitmq.username=${RABBITMQ_USERNAME}
spring.rabbitmq.password=${RABBITMQ_PASSWORD}
spring.rabbitmq.listener.simple.auto-startup=false
```

- [ ] **Step 6: Run property contract tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnvironmentConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: `EnvironmentConfigurationTest` passes.

---

### Task 6: Update Kubernetes and documentation

**Files:**
- Modify: `k8s/configmap.yaml`
- Modify: `k8s/secret.yaml`
- Modify: `README.md`

- [ ] **Step 1: Update ConfigMap middleware variables**

In `k8s/configmap.yaml`, delete:

```yaml
  RABBITMQ_HOST: "rabbitmq"
  RABBITMQ_PORT: "5672"
  RABBITMQ_VHOST: "/"
```

Add:

```yaml
  REDIS_ENABLED: "false"
  ROCKETMQ_ENABLED: "false"
  KAFKA_ENABLED: "false"
  MQTT_ENABLED: "false"
  QUARTZ_ENABLED: "false"

  ROCKETMQ_NAME_SERVER: "rocketmq-namesrv:9876"
  ROCKETMQ_PRODUCER_GROUP: "heartbeat-prod"
  ROCKETMQ_CONSUMER_GROUP: "heartbeat-prod"
  MQTT_HOST: "mqtt"
  MQTT_PORT: "1883"
```

- [ ] **Step 2: Update Secret middleware variables**

In `k8s/secret.yaml`, delete:

```yaml
  RABBITMQ_USERNAME: "guest"
  RABBITMQ_PASSWORD: "guest"
```

Add:

```yaml
  ROCKETMQ_ACCESS_KEY: ""
  ROCKETMQ_SECRET_KEY: ""
  MQTT_USERNAME: ""
  MQTT_PASSWORD: ""
```

- [ ] **Step 3: Update README quick-start and middleware sections**

Update `README.md` so it states:

```markdown
### 可选中间件开关

Redis、RocketMQ、Kafka、MQTT、Quartz 默认全部关闭：

```properties
REDIS_ENABLED=false
ROCKETMQ_ENABLED=false
KAFKA_ENABLED=false
MQTT_ENABLED=false
QUARTZ_ENABLED=false
```

未开启时应用不会创建对应客户端、监听器或 Quartz 调度器，也不会因为本机没有这些外部服务而启动失败。每个 profile 支持自己的地址变量，例如 `DEV_ROCKETMQ_NAME_SERVER`、`TEST_KAFKA_BOOTSTRAP_SERVERS`、`PROD_REDIS_HOST`；未配置 profile 专属变量时回退到通用变量。

RabbitMQ 已替换为 RocketMQ。开启 RocketMQ 时至少配置：

```properties
ROCKETMQ_ENABLED=true
ROCKETMQ_NAME_SERVER=rocketmq-namesrv:9876
ROCKETMQ_PRODUCER_GROUP=heartbeat-prod
```
```

- [ ] **Step 4: Scan for RabbitMQ remnants**

Run:

```powershell
rg -n "RabbitMQ|rabbitmq|RABBITMQ|spring\.rabbitmq|spring-boot-starter-amqp" -g "!**/target/**" -g "!**/node_modules/**"
```

Expected: no active runtime/config/dependency references remain. Historical design/plan docs may still mention RabbitMQ.

---

### Task 7: Final verification

**Files:**
- Verify all modified source, config, deployment, and documentation files.

- [ ] **Step 1: Run focused tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnvironmentConfigurationTest,MiddlewareAutoConfigurationImportFilterTest,MiddlewareToggleContextTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: focused tests pass.

- [ ] **Step 2: Run module test suite**

Run:

```powershell
mvn --% -pl heartbeat-start -am test
```

Expected: build and tests pass. If unrelated existing tests fail, capture exact failures and run the focused tests again to separate this change from existing project state.

- [ ] **Step 3: Verify RabbitMQ removal and RocketMQ presence**

Run:

```powershell
rg -n "spring\.rabbitmq|RABBITMQ_|spring-boot-starter-amqp" -g "!**/target/**" -g "!**/node_modules/**"
rg -n "rocketmq|ROCKETMQ|rocketmq-spring-boot-starter" -g "!**/target/**" -g "!**/node_modules/**"
```

Expected: RabbitMQ active references are gone, RocketMQ config/dependency/docs references exist.

- [ ] **Step 4: Final handoff**

Report:

```text
Implemented RabbitMQ -> RocketMQ and default-off middleware switches.
Focused verification: <command and pass/fail>.
Full verification: <command and pass/fail or reason not completed>.
Note: .git is an empty directory in this workspace, so no commit was created.
```

---

## Self-review

- Spec coverage:
  - RabbitMQ replacement is covered by Tasks 2, 5, and 6.
  - Default-off switches are covered by Tasks 1, 3, 5, and 7.
  - Environment-specific endpoints are covered by Tasks 1 and 5.
  - Startup without external services is covered by Tasks 3, 4, and the context test in Task 1.
  - Kubernetes and docs are covered by Task 6.
- Placeholder scan:
  - The plan does not contain unresolved implementation placeholders.
- Type and property consistency:
  - Switch keys consistently use `heartbeat.middleware.<name>.enabled`.
  - RocketMQ keys match the 2.2.0 starter configuration metadata: `rocketmq.name-server`, `rocketmq.producer.*`, and `rocketmq.consumer.*`.
