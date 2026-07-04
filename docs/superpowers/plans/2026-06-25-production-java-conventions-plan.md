# Production Java Conventions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 全量治理生产 Java 代码，使 Spring Bean 使用 `@Resource`、常用空判断使用 Apache Commons、样板访问器使用 Lombok、强类型字段映射优先使用 MapStruct。

**Architecture:** 先建立源码级规范测试作为迁移护栏，再按依赖注入、Commons、Lombok、MapStruct 四条改造线分阶段推进。每阶段先运行针对性测试确认失败，再进行最小批量修改并编译；MapStruct 按业务模块建立 Spring Mapper，不替代 JSON、动态 Map 或领域业务行为。

**Tech Stack:** Java 8、Spring Boot 2.7、Maven、JUnit 5、MapStruct 1.5.5、Lombok 1.18.30、Apache Commons Lang 3、Apache Commons Collections 4。

---

### Task 1: 建立生产代码规范护栏

**Files:**
- Create: `heartbeat-start/src/test/java/top/kx/heartbeat/conventions/ProductionCodeConventionTest.java`
- Modify: `pom.xml`
- Modify: relevant module `pom.xml` files

- [ ] **Step 1: 编写失败的源码规范测试**

测试扫描各模块 `src/main/java`，验证：

```java
assertNoProductionAutowired();
assertNoSpringComponentRequiredArgsConstructor();
assertNoSpringComponentBeanDependencyConstructor();
```

测试仅把 Spring 管理组件中的 Bean 注入构造器判为违规，不误伤 DTO、领域对象、`@Bean` 方法参数或 `@Value` 构造器。

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
mvn -pl heartbeat-start -am -Dtest=ProductionCodeConventionTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，报告当前生产代码中的 `@Autowired`、`@RequiredArgsConstructor` 和 Bean 依赖构造器。

- [ ] **Step 3: 补齐工具依赖**

在父 POM dependency management 中声明：

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-collections4</artifactId>
</dependency>
```

在需要使用的生产模块中加入依赖，不改变 MapStruct 与 Lombok 现有 annotation processor 配置。

- [ ] **Step 4: 运行依赖解析和测试编译**

Run:

```powershell
mvn -pl heartbeat-start -am -DskipTests test-compile
```

Expected: BUILD SUCCESS；规范测试仍因现有违规而失败。

### Task 2: 生产 Spring Bean 全量迁移到 `@Resource`

**Files:**
- Modify: `heartbeat-application/src/main/java/**/*.java`
- Modify: `heartbeat-infrastructure/src/main/java/**/*.java`
- Modify: `heartbeat-interfaces/src/main/java/**/*.java`
- Modify: `heartbeat-start/src/main/java/**/*.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/conventions/ProductionCodeConventionTest.java`

- [ ] **Step 1: 迁移 Lombok 构造器注入组件**

对 Spring 管理组件将：

```java
@RequiredArgsConstructor
class ExampleService {
    private final Repository repository;
}
```

改为：

```java
class ExampleService {
    @Resource
    private Repository repository;
}
```

删除 `@RequiredArgsConstructor`、对应 import 和 Bean 字段的 `final`。

- [ ] **Step 2: 迁移显式 Bean 构造器**

删除仅用于 Spring Bean 依赖注入的显式构造器，把参数改成 `@Resource` 字段。保留：

- `@Value` 配置构造器；
- DTO、领域和值对象构造器；
- `@Bean` 方法参数；
- 含业务校验和领域不变量的构造器。

- [ ] **Step 3: 处理集合与策略注册表**

对 `List<Strategy>` 注入使用：

```java
@Resource
private List<Strategy> strategies;

@PostConstruct
public void initializeRegistry() {
    // 建立不可变或受控索引
}
```

不直接对派生 `Map` 使用 `@Resource`，避免 Spring 将其解释为 Bean 名到实例的容器映射后改变现有 key 语义。

- [ ] **Step 4: 处理命名 Bean 和 Quartz**

命名 Bean 使用：

```java
@Resource(name = "socialRestTemplate")
private RestTemplate restTemplate;
```

Quartz Job 的 `@Autowired` 改为 `@Resource`，并同步修改相关注释。

- [ ] **Step 5: 运行规范测试**

Run:

```powershell
mvn -pl heartbeat-start -am -Dtest=ProductionCodeConventionTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 注入相关断言 PASS。

- [ ] **Step 6: 编译全部生产模块**

Run:

```powershell
mvn -DskipTests package
```

Expected: BUILD SUCCESS。

### Task 3: 全量迁移字符串、集合和 Map 判断

**Files:**
- Modify: all production Java source files containing manual string/collection/map emptiness checks
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/conventions/ProductionCodeConventionTest.java`

- [ ] **Step 1: 扩展失败的规范测试**

增加针对生产源码中以下典型写法的检测：

```text
value == null || value.trim().isEmpty()
value != null && !value.trim().isEmpty()
collection == null || collection.isEmpty()
map == null || map.isEmpty()
```

Expected replacements:

```java
StringUtils.isBlank(value)
StringUtils.isNotBlank(value)
CollectionUtils.isEmpty(values)
CollectionUtils.isNotEmpty(values)
MapUtils.isEmpty(values)
MapUtils.isNotEmpty(values)
```

- [ ] **Step 2: 运行测试并确认 RED**

Run the focused convention test and confirm it reports existing manual checks.

- [ ] **Step 3: 分模块替换**

按 application、domain、infrastructure、interfaces、start 顺序替换。保留：

- 普通对象 null 判断；
- Optional 判断；
- 数组长度判断；
- 字符串协议解析中对精确长度或字符位置的判断；
- 业务上区分 null 与空集合/空字符串的逻辑。

- [ ] **Step 4: 运行规范测试和模块测试**

Run:

```powershell
mvn -pl heartbeat-start -am -Dtest=ProductionCodeConventionTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -DskipTests package
```

Expected: Commons 相关断言 PASS，编译成功。

### Task 4: 清理可安全替换的 Getter/Setter/Builder 样板代码

**Files:**
- Modify: production DTO、command、entity、value carrier and model files with pure accessor boilerplate
- Test: existing compile and serialization tests

- [ ] **Step 1: 建立候选清单**

只选择满足以下条件的类：

- Getter/Setter 仅返回或赋值字段；
- Builder 仅赋值字段；
- 构造器不执行校验、归一化、状态迁移或副作用。

- [ ] **Step 2: 小批量使用 Lombok**

按职责选择：

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

或：

```java
@Value
@Builder
```

领域对象若不应公开 Setter，只使用 `@Getter`，保留领域行为方法。

- [ ] **Step 3: 每批执行编译**

Run:

```powershell
mvn -DskipTests package
```

Expected: BUILD SUCCESS，Jackson、MyBatis-Flex 和 MapStruct 所需构造器仍可用。

### Task 5: application 与 interfaces 强类型转换迁移到 MapStruct

**Files:**
- Create/Modify: `heartbeat-application/src/main/java/**/mapper/*Mapper.java`
- Create/Modify: `heartbeat-interfaces/src/main/java/**/mapper/*Mapper.java`
- Modify: hand-written assembler/converter call sites
- Test: focused mapper tests under corresponding module tests

- [ ] **Step 1: 识别纯字段映射**

优先处理现有 `Assembler`、`toDTO`、`fromRequest`、逐字段 setter 转换。动态 `Map<String, Object>` 不在本任务范围。

- [ ] **Step 2: 为每个 Mapper 编写失败测试**

示例：

```java
@Test
void mapsUserDomainToDto() {
    UserDTO dto = mapper.toDto(user);
    assertEquals(user.getId(), dto.getId());
    assertEquals(user.getEmail().value(), dto.getEmail());
}
```

先运行并确认 Mapper 尚不存在或行为尚未满足。

- [ ] **Step 3: 创建 Spring MapStruct Mapper**

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserModelMapper {
    @Mapping(target = "email", source = "email.value")
    UserDTO toDto(User user);
}
```

调用方通过 `@Resource` 注入 Mapper。

- [ ] **Step 4: 删除被替代的纯手写字段复制**

保留包含领域规则、异常转换、脱敏和动态结构处理的方法。

- [ ] **Step 5: 运行 Mapper 测试与模块编译**

Run focused mapper tests, then:

```powershell
mvn -pl heartbeat-interfaces -am test
```

Expected: mapper tests and dependent module tests PASS。

### Task 6: infrastructure 强类型 Entity/Domain 转换迁移到 MapStruct

**Files:**
- Create: `heartbeat-infrastructure/src/main/java/**/mapper/model/*ModelMapper.java`
- Modify: repositories containing `toEntity` / `toDomain`
- Test: repository and mapper tests

- [ ] **Step 1: 分类转换**

将转换拆为：

- 可由 MapStruct 完成的基础字段映射；
- JSON 字段专用方法；
- 加密/解密专用方法；
- 领域工厂或状态恢复逻辑。

- [ ] **Step 2: 为基础字段映射编写失败测试**

每个 Mapper 覆盖双向转换和字段改名；对 JSON、加密字段验证仍由原专用逻辑处理。

- [ ] **Step 3: 建立 Mapper**

使用：

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
```

必要时通过 `uses` 引入无副作用的值对象转换器，不使用大量内联 `expression`。

- [ ] **Step 4: 替换 repository 手写映射**

Repository 负责持久化编排，Mapper 负责基础字段复制，领域工厂负责恢复不变量。

- [ ] **Step 5: 运行 infrastructure 测试**

Run:

```powershell
mvn -pl heartbeat-infrastructure -am test
```

Expected: BUILD SUCCESS and tests PASS。

### Task 7: 最终静态扫描与全量验证

**Files:**
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/conventions/ProductionCodeConventionTest.java`
- Modify: any remaining violating production source

- [ ] **Step 1: 扫描残留**

Run searches for:

```text
@Autowired
@RequiredArgsConstructor
Spring component Bean constructors
manual blank checks
manual collection/map empty checks
pure accessor boilerplate candidates
pure field-copy conversion methods
```

- [ ] **Step 2: 修复真实残留并记录合理例外**

例外必须是明确的框架要求、领域行为、JSON/动态 Map 或业务语义差异，不通过宽泛排除规则隐藏违规。

- [ ] **Step 3: 运行全量测试**

Run:

```powershell
mvn test
```

Expected: BUILD SUCCESS，0 failures，0 errors。

- [ ] **Step 4: 运行打包验证**

Run:

```powershell
mvn -DskipTests package
```

Expected: BUILD SUCCESS。

- [ ] **Step 5: 检查应用上下文**

Run the existing Spring context/integration tests, including transaction manager, security, admin, workflow, payment and mobile suites.

Expected: Spring Bean 注入、MapStruct Mapper 注入、Quartz Job 注入和数据库上下文均能正常创建。
