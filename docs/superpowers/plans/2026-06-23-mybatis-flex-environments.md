# MyBatis-Flex Persistence and Environment Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the remaining JPA persistence with MyBatis-Flex and provide complete local, development, test, pre-release, gray, and production configuration.

**Architecture:** Keep domain repository interfaces unchanged and replace only infrastructure adapters. Centralize shared server, datasource pool, ORM, messaging, scheduling, and observability defaults in `application.yml`, while profile files contain environment-specific endpoints, credentials, pool sizes, and release behavior.

**Tech Stack:** Spring Boot 2.7.18, Java 8, MyBatis-Flex 1.11.7, MySQL 8, H2, HikariCP, Redis, RabbitMQ, Kafka, MQTT, Quartz, JUnit 5.

---

### Task 1: Add persistence architecture regression tests

**Files:**
- Create: `heartbeat-start/src/test/java/top/kx/heartbeat/config/MybatisFlexOnlyPersistenceTest.java`

- [ ] Write a Spring Boot integration test using the `local` profile.
- [ ] Assert that `entityManagerFactory` and Spring Data JPA are absent.
- [ ] Exercise user save, lookup, and update through the domain repository.
- [ ] Run the test and verify that it fails because JPA is still present.

### Task 2: Add environment configuration regression tests

**Files:**
- Create: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnvironmentConfigurationTest.java`

- [ ] Load the YAML files with `YamlPropertySourceLoader`.
- [ ] Assert port `7001`, default `dev`, middleware defaults, and the Spring Boot 2.7 Redis prefix.
- [ ] Assert MySQL for `dev/test/pre/gray/prod`, H2 for `local`, and no JPA properties.
- [ ] Run the test and verify the expected failures against the current configuration.

### Task 3: Migrate the user repository to MyBatis-Flex

**Files:**
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/user/persistence/UserPO.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper/UserMapper.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/user/repository/UserRepositoryImpl.java`
- Delete: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/user/persistence/UserJpaRepository.java`

- [ ] Replace JPA annotations with MyBatis-Flex annotations.
- [ ] Add the base mapper.
- [ ] Implement insert/update and query operations with `BaseMapper` and `QueryWrapper`.
- [ ] Run the persistence test and verify it passes.

### Task 4: Remove the remaining JPA stack

**Files:**
- Modify: `heartbeat-infrastructure/pom.xml`
- Delete: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/structure/persistence/StructureDefinitionJpaRepository.java`
- Delete: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/structure/persistence/StructureDefinitionPO.java`
- Delete: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/structure/persistence/StructureVersionPO.java`
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/config/TransactionManagerConfigurationTest.java`

- [ ] Remove `spring-boot-starter-data-jpa`.
- [ ] Remove unused JPA persistence classes.
- [ ] Pin the transaction manager test to `local`.
- [ ] Search the source tree for JPA imports and verify none remain.

### Task 5: Complete common and environment configuration

**Files:**
- Modify: `heartbeat-infrastructure/pom.xml`
- Modify: `heartbeat-start/src/main/resources/application.yml`
- Modify: `heartbeat-start/src/main/resources/application-local.yml`
- Modify: `heartbeat-start/src/main/resources/application-dev.yml`
- Modify: `heartbeat-start/src/main/resources/application-test.yml`
- Modify: `heartbeat-start/src/main/resources/application-pre.yml`
- Modify: `heartbeat-start/src/main/resources/application-gray.yml`
- Modify: `heartbeat-start/src/main/resources/application-prod.yml`

- [ ] Add Redis, RabbitMQ, Kafka, MQTT, and connection-pool dependencies used by the configuration.
- [ ] Put shared pool, messaging, MyBatis-Flex, Quartz, health, and lifecycle defaults in `application.yml`.
- [ ] Configure profile-specific MySQL/H2 endpoints, pool sizes, Redis databases, Quartz mode, and security switches.
- [ ] Remove all `spring.jpa` and Hibernate logging properties.
- [ ] Run the YAML regression test and verify it passes.

### Task 6: Verify the backend

**Files:**
- Verify all modified backend files.

- [ ] Run the new configuration and persistence tests.
- [ ] Run existing transaction, admin, security, and backend tool tests.
- [ ] Run `mvn -pl heartbeat-start -am package -DskipTests`.
- [ ] Run a final search for JPA/JdbcTemplate business persistence remnants.
- [ ] Report any pre-existing unrelated test failure separately.

