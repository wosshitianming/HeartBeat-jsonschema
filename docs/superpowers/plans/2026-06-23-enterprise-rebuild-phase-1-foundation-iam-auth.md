# Enterprise Rebuild Phase 1: Foundation, IAM and Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the generic platform resource model with Flyway-managed, dedicated tenant/IAM/auth/config/audit tables and typed MyBatis-Flex services while preserving the existing front-end API paths.

**Architecture:** Production DDL is owned by Flyway and uses MySQL 8 semantics. Platform capabilities are split into tenant, organization, IAM, authentication, configuration and audit units; compatibility controllers translate legacy request/response fields into typed application commands. Tenant identity is established by authentication rather than an untrusted request header.

**Tech Stack:** Java 8, Spring Boot 2.7.18, MyBatis-Flex 1.11.7, MySQL 8.0, Flyway, Spring Security, BCrypt, Redis, JUnit 5, Testcontainers MySQL.

---

## File map

### Database and configuration

- Modify `pom.xml`: manage Testcontainers version.
- Modify `heartbeat-infrastructure/pom.xml`: add Flyway.
- Modify `heartbeat-start/pom.xml`: add Testcontainers MySQL test dependencies.
- Modify `heartbeat-start/src/main/resources/application.yml`: enable MySQL Flyway and configure migration validation.
- Modify `heartbeat-start/src/main/resources/application-local.yml`: retain H2 only for lightweight/local tests and disable Flyway there.
- Modify `heartbeat-start/src/main/resources/schema.sql`: replace generic platform tables with explicit Phase 1 H2-compatible tables.
- Create `heartbeat-start/src/main/resources/db/migration/mysql/V1__enterprise_platform_iam_auth.sql`.
- Create `heartbeat-start/src/main/resources/db/migration/mysql/V2__platform_seed.sql`.
- Create `heartbeat-start/src/main/resources/db/migration/mysql/V3__permission_seed.sql`.

### Domain and application contracts

- Create packages under `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/`:
  `tenant`, `organization`, `iam`, `auth`, `config`, `audit`.
- Create typed IDs/enums/models and repository interfaces in those packages.
- Create application commands, queries and DTOs under
  `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/`.
- Split `AdminPlatformService` into focused services:
  `TenantApplicationService`, `OrganizationApplicationService`,
  `IamApplicationService`, `AuthenticationApplicationService`,
  `ConfigurationApplicationService`.

### Persistence

- Create dedicated entities under
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/platform/`.
- Create dedicated mappers under
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper/platform/`.
- Create repositories under:
  `infrastructure/platform/tenant`, `organization`, `iam`, `auth`, `config`, `audit`.
- Replace `OperLogAspect` persistence with `OperationLogRepository`.
- Replace generic role/menu/user resource operations in `AdminPlatformFlexRepository`.

### Security and interfaces

- Create `AuthenticatedPrincipal`.
- Modify `JwtTokenService`, `JwtAuthenticationFilter`, `SecurityConfig`,
  `SpringSecurityCurrentUserProvider`, `PermissionGuard`.
- Delete `TenantContextFilter` after JWT-established tenant context is active.
- Create typed IAM controllers under
  `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/iam`.
- Keep `AdminResourceController` as a compatibility adapter only.
- Add request/response DTO packages for tenant, organization, IAM, auth and config.

### Tests

- Create `EnterpriseDatabaseContractTest`.
- Create `EnterpriseMySqlMigrationTest`.
- Replace generic-resource assertions in `AdminPlatformApiTest`.
- Replace direct `PlatformResourceMapper` setup in `SecurityAuthorizationApiTest`.
- Add `TenantIsolationSecurityTest`, `PasswordSecurityTest`,
  `AuthenticationSessionTest`, and `AuditPersistenceTest`.

## Task 1: Add migration and MySQL integration-test infrastructure

**Files:**
- Modify: `pom.xml`
- Modify: `heartbeat-infrastructure/pom.xml`
- Modify: `heartbeat-start/pom.xml`
- Modify: `heartbeat-start/src/main/resources/application.yml`
- Modify: `heartbeat-start/src/main/resources/application-local.yml`

- [ ] **Step 1: Add the Testcontainers version**

Add to the root `<properties>`:

```xml
<testcontainers.version>1.19.8</testcontainers.version>
```

Add to root `<dependencyManagement>`:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-bom</artifactId>
    <version>${testcontainers.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

- [ ] **Step 2: Add Flyway**

Add to `heartbeat-infrastructure/pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

- [ ] **Step 3: Add MySQL integration-test dependencies**

Add to `heartbeat-start/pom.xml`:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 4: Configure production migrations**

Add under `spring` in `application.yml`:

```yaml
  flyway:
    enabled: true
    locations: classpath:db/migration/mysql
    baseline-on-migrate: false
    validate-on-migrate: true
    clean-disabled: true
    out-of-order: false
    encoding: UTF-8
```

Keep `spring.sql.init.mode: never`.

- [ ] **Step 5: Keep local H2 isolated**

Add under `spring` in `application-local.yml`:

```yaml
  flyway:
    enabled: false
```

H2 remains a local/lightweight test database during the staged rebuild. Production schema acceptance comes only from MySQL migrations.

- [ ] **Step 6: Verify dependency resolution**

Run:

```powershell
mvn --% -pl heartbeat-start -am -DskipTests compile
```

Expected: `BUILD SUCCESS`.

## Task 2: Establish the enterprise SQL contract before writing DDL

**Files:**
- Create: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnterpriseDatabaseContractTest.java`
- Modify: `heartbeat-start/src/test/java/top/kx/heartbeat/config/DatabaseSqlOrganizationTest.java`

- [ ] **Step 1: Write a failing migration contract test**

Create a test that loads `db/migration/mysql/V1__enterprise_platform_iam_auth.sql` and asserts:

```java
assertFalse(sql.contains("sys_resource_base"));
assertFalse(sql.toUpperCase(Locale.ROOT).contains("CREATE TABLE") && sql.toUpperCase(Locale.ROOT).contains(" LIKE "));
assertTrue(sql.contains("`sys_tenant`"));
assertTrue(sql.contains("`sys_dept`"));
assertTrue(sql.contains("`sys_post`"));
assertTrue(sql.contains("`sys_user`"));
assertTrue(sql.contains("`sys_user_post`"));
assertTrue(sql.contains("`sys_role`"));
assertTrue(sql.contains("`sys_permission`"));
assertTrue(sql.contains("`sys_menu`"));
assertTrue(sql.contains("`sys_user_role`"));
assertTrue(sql.contains("`sys_role_permission`"));
assertTrue(sql.contains("`sys_menu_permission`"));
assertTrue(sql.contains("`sys_role_dept`"));
assertTrue(sql.contains("`sys_dict_type`"));
assertTrue(sql.contains("`sys_dict_item`"));
assertTrue(sql.contains("`sys_config`"));
assertTrue(sql.contains("`sys_notice`"));
assertTrue(sql.contains("`sys_user_preference`"));
assertTrue(sql.contains("`auth_oauth_client`"));
assertTrue(sql.contains("`auth_client_grant`"));
assertTrue(sql.contains("`auth_client_redirect_uri`"));
assertTrue(sql.contains("`auth_social_provider`"));
assertTrue(sql.contains("`auth_social_binding`"));
assertTrue(sql.contains("`auth_session`"));
assertTrue(sql.contains("`sys_oper_log`"));
assertTrue(sql.contains("`sys_login_log`"));
```

Add dedicated-column assertions:

```java
assertTableContains(sql, "sys_dept", "dept_code", "dept_name", "ancestors", "dept_level", "leader_user_id");
assertTableContains(sql, "sys_role", "role_code", "role_name", "role_type", "data_scope");
assertTableContains(sql, "sys_config", "config_key", "config_value", "value_type", "encrypted");
assertTableContains(sql, "sys_oper_log", "request_path", "request_method", "operator_id", "duration_ms");
assertTableContains(sql, "auth_oauth_client", "client_id", "client_secret_hash", "access_token_ttl", "refresh_token_ttl");
```

Add primary-key assertions:

```java
assertTrue(sql.matches("(?s).*`id`\\s+BIGINT\\s+UNSIGNED\\s+NOT\\s+NULL\\s+AUTO_INCREMENT.*"));
assertFalse(sql.matches("(?s).*`id`\\s+VARCHAR\\(.*"));
```

- [ ] **Step 2: Retire the old aggregate-layout assumptions**

Change `DatabaseSqlOrganizationTest` so it no longer requires the old module scripts and
`schema.sql` to expose the same generic table set. Retain only checks that legacy compatibility
files contain no real credentials and clearly identify themselves as deprecated.

- [ ] **Step 3: Run the contract test**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnterpriseDatabaseContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because `V1__enterprise_platform_iam_auth.sql` does not exist.

## Task 3: Create dedicated Phase 1 MySQL tables

**Files:**
- Create: `heartbeat-start/src/main/resources/db/migration/mysql/V1__enterprise_platform_iam_auth.sql`

- [ ] **Step 1: Add migration preamble**

Start the migration with:

```sql
SET NAMES utf8mb4;
SET time_zone = '+00:00';
```

Do not use `CREATE TABLE IF NOT EXISTS`; Flyway version history owns idempotency.

- [ ] **Step 2: Create tenant and plan tables**

Create:

```text
sys_tenant_plan
sys_plan_feature
sys_tenant
sys_tenant_feature
```

Use auto-increment IDs. `sys_tenant` must not have `tenant_id`. Enforce:

```text
uk_tenant_plan_code(plan_code, delete_marker)
uk_plan_feature_code(plan_id, feature_code)
uk_tenant_code(tenant_code, delete_marker)
uk_tenant_domain(domain, delete_marker)
uk_tenant_feature_code(tenant_id, feature_code)
```

- [ ] **Step 3: Create organization tables**

Create dedicated `sys_dept`, `sys_post`, and `sys_user_post`.

Required unique keys:

```text
uk_dept_code(tenant_id, dept_code, delete_marker)
uk_post_code(tenant_id, post_code, delete_marker)
uk_user_post(tenant_id, user_id, post_id)
```

Required department indexes:

```text
idx_dept_parent(tenant_id, parent_id, sort_no, id)
idx_dept_leader(tenant_id, leader_user_id)
```

- [ ] **Step 4: Create user and IAM tables**

Create:

```text
sys_user
sys_role
sys_permission
sys_menu
sys_user_role
sys_role_permission
sys_menu_permission
sys_role_dept
```

Required unique keys:

```text
uk_user_username(tenant_id, username, delete_marker)
uk_user_email(tenant_id, email, delete_marker)
uk_user_phone(tenant_id, phone, delete_marker)
uk_role_code(tenant_id, role_code, delete_marker)
uk_permission_code(tenant_id, permission_code, delete_marker)
uk_menu_code(tenant_id, menu_code, delete_marker)
uk_user_role(tenant_id, user_id, role_id)
uk_role_permission(tenant_id, role_id, permission_id)
uk_menu_permission(tenant_id, menu_id, permission_id)
uk_role_dept(tenant_id, role_id, dept_id)
```

Do not put `permission_code` on `sys_menu`.

- [ ] **Step 5: Create configuration tables**

Create:

```text
sys_dict_type
sys_dict_item
sys_config
sys_notice
sys_user_preference
```

Enforce tenant-scoped business keys:

```text
uk_dict_type_code(tenant_id, dict_code, delete_marker)
uk_dict_item_value(tenant_id, dict_type_id, item_value, delete_marker)
uk_config_key(tenant_id, config_key, delete_marker)
uk_user_preference(tenant_id, user_id, preference_key)
```

- [ ] **Step 6: Create authentication tables**

Create:

```text
auth_oauth_client
auth_client_grant
auth_client_redirect_uri
auth_social_provider
auth_social_binding
auth_session
```

Store only `client_secret_hash`, `app_secret_cipher`, and `refresh_token_hash`.
Required keys include:

```text
uk_oauth_client_id(tenant_id, client_id, delete_marker)
uk_social_provider_code(tenant_id, provider_code, delete_marker)
uk_social_binding_external(tenant_id, provider_id, external_user_id)
uk_auth_session_no(tenant_id, session_id)
idx_auth_session_user(tenant_id, user_id, status, expire_at)
```

- [ ] **Step 7: Create append-only audit tables**

Create `sys_oper_log` and `sys_login_log` without `version` or `delete_marker`.
Add indexes:

```text
idx_oper_log_operator(tenant_id, operator_id, operated_at)
idx_oper_log_trace(tenant_id, trace_id)
idx_login_log_user(tenant_id, user_id, logged_at)
idx_login_log_username(tenant_id, username, logged_at)
idx_login_log_trace(tenant_id, trace_id)
```

- [ ] **Step 8: Run the SQL contract**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnterpriseDatabaseContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

## Task 4: Add deterministic platform and permission seed migrations

**Files:**
- Create: `heartbeat-start/src/main/resources/db/migration/mysql/V2__platform_seed.sql`
- Create: `heartbeat-start/src/main/resources/db/migration/mysql/V3__permission_seed.sql`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnterpriseDatabaseContractTest.java`

- [ ] **Step 1: Add seed-security assertions**

Assert:

```java
String seed = resource("db/migration/mysql/V2__platform_seed.sql");
assertFalse(seed.contains("'admin123'"));
assertFalse(seed.contains("'123456'"));
assertTrue(seed.contains("$2"));
assertFalse(seed.toLowerCase(Locale.ROOT).contains("client-secret"));
```

- [ ] **Step 2: Seed the default platform records**

`V2__platform_seed.sql` creates:

```text
default plan: ENTERPRISE
default tenant: heartbeat
root department: platform
administrator: admin
super administrator role: super_admin
administrator-role relation
default system name configuration
```

Use fixed numeric IDs only for baseline records so relationships remain deterministic.
Store a BCrypt development bootstrap hash supplied through the migration, document that production
must rotate it immediately, and never store the plaintext in SQL comments.

- [ ] **Step 3: Seed menu and permission resources separately**

`V3__permission_seed.sql` creates:

```text
menus required by the current web application
dedicated list/add/edit/remove permissions
menu-permission relations
super-admin role-permission relations
```

Every Controller permission string used in source must have a corresponding
`sys_permission.permission_code`.

- [ ] **Step 4: Run the seed contract**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnterpriseDatabaseContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

## Task 5: Prove migrations on a real MySQL 8 container

**Files:**
- Create: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnterpriseMySqlMigrationTest.java`

- [ ] **Step 1: Write the container migration test**

Use:

```java
@Testcontainers(disabledWithoutDocker = true)
class EnterpriseMySqlMigrationTest {
    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.36")
                    .withDatabaseName("heartbeat")
                    .withUsername("heartbeat")
                    .withPassword("heartbeat-test");
}
```

Create a Flyway instance with the container JDBC URL and migrate
`classpath:db/migration/mysql`.

- [ ] **Step 2: Assert actual metadata**

Query `information_schema.columns` and assert:

```text
sys_dept.dept_code exists
sys_role.data_scope exists
sys_config.config_value exists
auth_session.refresh_token_hash exists
sys_oper_log.duration_ms exists
sys_resource_base does not exist
all Phase 1 id columns are bigint unsigned auto_increment
```

- [ ] **Step 3: Run the migration test**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnterpriseMySqlMigrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected with Docker: PASS.  
Expected without Docker: skipped with the reason reported by Testcontainers.

## Task 6: Align the local H2 schema for Phase 1 tests

**Files:**
- Modify: `heartbeat-start/src/main/resources/schema.sql`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/config/EnterpriseDatabaseContractTest.java`

- [ ] **Step 1: Remove the generic platform block**

Delete from local schema:

```text
users
sys_resource_base
all copied sys_* resource tables
hb_social_provider
hb_social_bind
```

- [ ] **Step 2: Add H2-compatible explicit Phase 1 tables**

Mirror the MySQL table and column names for all Phase 1 tables.
Use:

```sql
id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY
```

Use `TEXT` for MySQL JSON columns only in H2. Keep unique constraints and relationship indexes.
Retain non-Phase-1 structure/flow/business tables temporarily so unrelated existing tests can still run.

- [ ] **Step 3: Add table/column parity checks**

Extend `EnterpriseDatabaseContractTest` to compare the Phase 1 table names and normalized column
names between MySQL V1 and `schema.sql`.

- [ ] **Step 4: Run configuration tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnterpriseDatabaseContractTest,EnvironmentConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

## Task 7: Introduce typed tenant and identity context

**Files:**
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/tenant/TenantId.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/iam/UserId.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/security/AuthenticatedPrincipal.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/tenant/TenantContext.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/tenant/HeartbeatTenantFactory.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/security/TenantIsolationSecurityTest.java`

- [ ] **Step 1: Write failing context tests**

Test that:

```text
TenantContext.getRequiredTenantId() throws when no tenant is authenticated
TenantContext.runAsPlatform executes without tenant filtering only through an explicit platform scope
Tenant IDs are Long values, not arbitrary strings
```

- [ ] **Step 2: Implement immutable ID value objects**

Both IDs expose:

```java
public static TenantId of(long value)
public long value()
```

Reject zero and negative values.

- [ ] **Step 3: Replace the default tenant fallback**

`TenantContext` must no longer return `"1"` when empty. Provide:

```java
public static Long getTenantId()
public static long getRequiredTenantId()
public static void setTenantId(long tenantId)
public static void clear()
public static <T> T runAsPlatform(Supplier<T> action)
```

- [ ] **Step 4: Update the MyBatis-Flex tenant factory**

Return an empty tenant array only in explicit platform scope; otherwise return the authenticated
tenant ID and fail closed when it is absent.

- [ ] **Step 5: Run context tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=TenantIsolationSecurityTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

## Task 8: Add dedicated platform entities and mappers

**Files:**
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/entity/platform/*.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/persistence/mapper/platform/*.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/admin/PlatformPersistenceMappingTest.java`

- [ ] **Step 1: Write failing mapping tests**

For each Phase 1 table, assert its entity has:

```text
the correct @Table name
Long id
the dedicated business fields expected by V1
@Column(tenantId = true) only on tenant-private entities
@Version on mutable aggregate version fields
```

- [ ] **Step 2: Create dedicated entities**

Create one entity per table. Representative examples:

```java
@Table("sys_dept")
public class SysDeptEntity {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column(tenantId = true)
    private Long tenantId;
    private Long parentId;
    private String deptCode;
    private String deptName;
    private String ancestors;
    private Integer deptLevel;
    private Long leaderUserId;
    private String phone;
    private String email;
    private Integer sortNo;
    private String status;
    @Column(version = true)
    private Integer version;
    private Long deleteMarker;
    private Long createdBy;
    private LocalDateTime createTime;
    private Long updatedBy;
    private LocalDateTime updateTime;
}
```

```java
@Table("sys_oper_log")
public class SysOperLogEntity {
    @Id(keyType = KeyType.Auto)
    private Long id;
    @Column(tenantId = true)
    private Long tenantId;
    private String traceId;
    private String moduleCode;
    private String operationType;
    private String operationName;
    private Long operatorId;
    private String operatorName;
    private String requestMethod;
    private String requestPath;
    private String requestIp;
    private String requestParams;
    private String responseSummary;
    private String resultStatus;
    private String errorCode;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime operatedAt;
}
```

- [ ] **Step 3: Create one BaseMapper per entity**

Each mapper has the form:

```java
public interface SysDeptMapper extends BaseMapper<SysDeptEntity> {
}
```

Use explicit annotated queries only for joins that cannot be expressed clearly through focused
repository query objects.

- [ ] **Step 4: Run mapping tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=PlatformPersistenceMappingTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

## Task 9: Replace generic organization and IAM repositories

**Files:**
- Create typed domain models and repository interfaces under:
  `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/organization/`
  and `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/iam/`
- Create repository implementations under:
  `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/organization/`
  and `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/iam/`
- Create application services under:
  `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/organization/`
  and `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/iam/`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/admin/OrganizationIamServiceTest.java`

- [ ] **Step 1: Write failing typed-service tests**

Cover:

```text
department code is unique inside one tenant and reusable in another tenant
department cannot be deleted while it has children
role code is tenant-scoped
role permissions and menu permissions are independent
custom data scope stores explicit department relations
user list honors ALL, DEPT, DEPT_AND_CHILD, SELF and CUSTOM scopes
```

- [ ] **Step 2: Define typed commands**

Create:

```text
CreateDepartmentCommand
UpdateDepartmentCommand
CreatePostCommand
UpdatePostCommand
CreateUserCommand
UpdateUserCommand
CreateRoleCommand
UpdateRoleCommand
AssignUserRolesCommand
AssignRolePermissionsCommand
AssignRoleDepartmentsCommand
CreateMenuCommand
UpdateMenuCommand
BindMenuPermissionsCommand
```

Use `Long` IDs, enums for statuses/data scopes, and Bean Validation in interface request DTOs.

- [ ] **Step 3: Define focused repository interfaces**

Repositories return typed models or projections:

```java
Optional<Department> findById(long tenantId, long id);
List<DepartmentSummary> findAll(long tenantId);
Department save(Department department);
boolean existsByCode(long tenantId, String code, long excludeId);
```

No repository method returns `Map<String, Object>`.

- [ ] **Step 4: Implement MyBatis-Flex repositories**

Every update/delete query must include both:

```text
id = ?
tenant_id = ?
```

Use logical delete markers for mutable master data and optimistic-lock versions for updates.

- [ ] **Step 5: Implement application services**

Application services own transaction boundaries and enforce:

```text
tenant access
uniqueness
parent/child constraints
role/data-scope invariants
permission/menu relation integrity
password creation policy
```

- [ ] **Step 6: Run focused tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=OrganizationIamServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

## Task 10: Replace password, JWT and session handling

**Files:**
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/security/JwtTokenService.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/security/JwtAuthenticationFilter.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/security/SecurityConfig.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/security/SpringSecurityCurrentUserProvider.java`
- Delete: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/tenant/TenantContextFilter.java`
- Create: typed authentication repository/service classes
- Test: `PasswordSecurityTest.java`
- Test: `AuthenticationSessionTest.java`

- [ ] **Step 1: Write failing password tests**

Assert:

```text
new users never persist raw password text
legacy plaintext hashes are rejected
BCrypt hashes match with strength >= 10
API responses never contain passwordHash
```

- [ ] **Step 2: Write failing session/JWT tests**

Assert JWT claims include:

```text
uid
tid
sid
token_type
```

Assert:

```text
access token restores user and tenant
revoked session rejects refresh
refresh token is stored only as a hash
X-Tenant-Id cannot change the authenticated tenant
```

- [ ] **Step 3: Create a typed authenticated principal**

`AuthenticatedPrincipal` contains:

```java
private final long userId;
private final long tenantId;
private final String sessionId;
private final String username;
```

- [ ] **Step 4: Establish tenant context from verified JWT**

After signature and session validation, `JwtAuthenticationFilter`:

```text
creates AuthenticatedPrincipal
sets TenantContext
loads permission authorities
clears SecurityContext and TenantContext in finally
```

Remove `TenantContextFilter` from `SecurityConfig`.

- [ ] **Step 5: Persist sessions**

On login:

```text
resolve tenant by tenantCode or configured default tenant
verify BCrypt password
create auth_session
issue JWT containing uid/tid/sid
record successful login
```

On logout, set `revoked_at` and `status = REVOKED`.

- [ ] **Step 6: Run security tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=PasswordSecurityTest,AuthenticationSessionTest,TenantIsolationSecurityTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

## Task 11: Replace social-provider persistence

**Files:**
- Create typed provider/binding models and repositories
- Modify: `heartbeat-application/src/main/java/top/kx/heartbeat/application/auth/SocialLoginService.java`
- Modify: social handler integrations only where typed configuration replaces maps
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/security/SocialAuthenticationTest.java`

- [ ] **Step 1: Write failing social-auth tests**

Cover:

```text
provider code is tenant-scoped
secret is encrypted at rest and masked in admin responses
external identity binding is unique per provider
auto-registration creates a BCrypt-protected local account
social login creates auth_session and tenant-bound JWT
```

- [ ] **Step 2: Replace provider Map contracts**

Introduce:

```text
SocialProviderConfiguration
ExternalUserProfile
SocialBinding
```

Update `SocialLoginHandler` so handler input/output is typed.

- [ ] **Step 3: Implement dedicated repositories**

Use `auth_social_provider` and `auth_social_binding`; remove all access to
`hb_social_provider`, `hb_social_bind`, `sys_social_provider`, and
`sys_user_social_bind`.

- [ ] **Step 4: Run social-auth tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=SocialAuthenticationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

## Task 12: Replace configuration and audit persistence

**Files:**
- Create typed config/dictionary/notice/preference services and repositories
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/audit/OperLogAspect.java`
- Create: `OperationLogRepository`, `LoginLogRepository`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/admin/ConfigurationServiceTest.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/security/AuditPersistenceTest.java`

- [ ] **Step 1: Write failing configuration tests**

Cover:

```text
config key uniqueness per tenant
typed BOOLEAN/INTEGER/STRING/JSON conversion
encrypted config values are masked
dictionary items belong to a dictionary type in the same tenant
user preferences are unique per user and key
```

- [ ] **Step 2: Write failing append-only audit tests**

Assert:

```text
operation log captures method, path, user, tenant, result and duration
login log captures success/failure and source IP
password, Authorization and secret fields are redacted
no audit update/delete service exists
```

- [ ] **Step 3: Implement typed services and repositories**

Use dedicated tables and DTOs. Do not expose generic update methods for logs.

- [ ] **Step 4: Rewrite `OperLogAspect`**

Build an `OperationLogRecord` and call `OperationLogRepository.append(record)`.
Use Jackson-based redaction rather than manually concatenating JSON strings.

- [ ] **Step 5: Run configuration/audit tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=ConfigurationServiceTest,AuditPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

## Task 13: Build standard typed APIs and legacy compatibility adapters

**Files:**
- Create typed controllers and request/response DTOs under:
  `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/iam/`,
  `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/platform/`,
  `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/auth/`,
  `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/config/`
- Modify: `AdminResourceController.java`
- Modify: `IamMenuController.java`
- Modify: `IamRoleController.java`
- Modify: `AuthController.java`
- Test: `AdminPlatformApiTest.java`

- [ ] **Step 1: Add standard API tests**

Test:

```text
/api/v1/iam/users
/api/v1/iam/departments
/api/v1/iam/posts
/api/v1/iam/roles
/api/v1/iam/permissions
/api/v1/iam/menus
/api/v1/system/dictionaries
/api/v1/system/configurations
/api/v1/system/notices
/api/v1/system/audit/operations
/api/v1/system/audit/logins
```

- [ ] **Step 2: Add legacy compatibility tests**

Retain existing front-end paths:

```text
/api/v1/admin/resources/users
/api/v1/admin/resources/depts
/api/v1/admin/resources/posts
/api/v1/admin/resources/roles
/api/v1/admin/resources/dict-types
/api/v1/admin/resources/dict-data
/api/v1/admin/resources/configs
/api/v1/admin/resources/notices
/api/v1/admin/resources/oper-logs
/api/v1/admin/resources/login-logs
/api/v1/admin/resources/social-providers
```

IDs in JSON responses must be strings.

- [ ] **Step 3: Implement typed controllers**

Controllers accept validated request DTOs and return response DTOs. They call only application
services.

- [ ] **Step 4: Convert `AdminResourceController` into an adapter**

Replace calls to `AdminPlatformService.listResource/createResource/updateResource/deleteResource`
with a fixed switch that delegates to typed services and compatibility assemblers.

The adapter must not accept unknown resources and must not derive table names.

- [ ] **Step 5: Run API tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=AdminPlatformApiTest,SecurityAuthorizationApiTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS with no direct generic-resource setup.

## Task 14: Remove the generic platform persistence model

**Files:**
- Delete: `PlatformResource.java`
- Delete: `PlatformResourceMapper.java`
- Delete: `PlatformFlexResourceSupport.java`
- Delete: `PlatformEntityConverter.java`
- Delete or rewrite: `AdminPlatformFlexRepository.java`
- Delete: old social entities/mappers
- Delete: legacy `users` domain demo persistence if no remaining API depends on it
- Modify: `AdminPlatformRepository.java`
- Modify: `AdminPlatformService.java`
- Test: `MybatisFlexOnlyPersistenceTest.java`

- [ ] **Step 1: Add forbidden-pattern assertions**

Assert source contains none of:

```text
PlatformResource
setHintTableMapping
sys_resource_base
CREATE TABLE ... LIKE
```

- [ ] **Step 2: Remove generic persistence classes**

Delete the classes only after all callers have been migrated to typed services.

- [ ] **Step 3: Remove generic repository methods**

Delete:

```java
listResource(String resource)
createResource(String resource, Map<String, Object> command)
updateResource(String resource, String id, Map<String, Object> command)
deleteResource(String resource, String id)
```

- [ ] **Step 4: Verify no dynamic table mapping remains**

Run:

```powershell
rg -n "PlatformResource|setHintTableMapping|sys_resource_base|CREATE TABLE.*LIKE" heartbeat-domain heartbeat-application heartbeat-infrastructure heartbeat-interfaces heartbeat-start
```

Expected: no matches except negative assertions in tests and historical design documentation.

## Task 15: Complete Phase 1 verification

**Files:**
- Modify: `README.md`
- Modify: `docs/saas-admin-mvp.md`
- Modify: environment configuration tests as required

- [ ] **Step 1: Document Flyway and bootstrap rules**

Document:

```text
MySQL 8 is required for production
Flyway owns schema changes
local H2 is non-production only
bootstrap administrator password must be rotated
ordinary requests cannot select tenant by header
legacy admin resource routes are compatibility-only
```

- [ ] **Step 2: Run focused Phase 1 tests**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=EnterpriseDatabaseContractTest,EnterpriseMySqlMigrationTest,PlatformPersistenceMappingTest,OrganizationIamServiceTest,PasswordSecurityTest,AuthenticationSessionTest,TenantIsolationSecurityTest,SocialAuthenticationTest,ConfigurationServiceTest,AuditPersistenceTest,AdminPlatformApiTest,SecurityAuthorizationApiTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all available tests PASS; Docker-only test may be skipped only when Docker is unavailable.

- [ ] **Step 3: Run the complete test suite**

Run:

```powershell
mvn --% -pl heartbeat-start -am test
```

Expected: PASS. Any pre-existing unrelated failure must be documented with its exact test and
must not be hidden by disabling the test.

- [ ] **Step 4: Package**

Run:

```powershell
mvn --% -pl heartbeat-start -am -DskipTests package
```

Expected: `BUILD SUCCESS` and `heartbeat-start/target/heartbeat.jar`.

- [ ] **Step 5: Verify local startup**

Run:

```powershell
java -jar heartbeat-start\target\heartbeat.jar --spring.profiles.active=local --spring.main.banner-mode=off
```

Expected:

```text
Tomcat started on port(s): 7001
Started HeartBeatApplication
```

Stop the process after verification.

## Non-Git workspace note

The current workspace is not a valid Git repository (`git rev-parse` fails). Therefore this plan
uses testable checkpoints rather than commit steps. If Git is initialized later, create one commit
after each completed task and never combine unrelated phases.
