# HeartBeat Database SQL Organization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the drifting MySQL SQL files with module-owned scripts, a complete one-click installer, synchronized compatibility files, and an automated schema contract test.

**Architecture:** MySQL 8.0 scripts are split by bounded context under `db/mysql`; `heartbeat-all.sql` is a materialized concatenation so database tools can execute it without `SOURCE`. H2 keeps its own compatible `schema.sql`, while a JUnit resource test enforces that MySQL modules, the aggregate installer, compatibility scripts, and H2 expose the same application table set.

**Tech Stack:** MySQL 8.0 SQL, H2 MySQL compatibility mode, Spring Boot 2.7.18, Java 8, JUnit 5, Maven.

---

### Task 1: Add the database script contract test

**Files:**
- Create: `heartbeat-start/src/test/java/top/kx/heartbeat/config/DatabaseSqlOrganizationTest.java`

- [ ] **Step 1: Write the failing test**

Create a JUnit test that:

```java
private static final List<String> MODULE_FILES = Arrays.asList(
        "db/mysql/01-structure.sql",
        "db/mysql/02-platform.sql",
        "db/mysql/03-flow.sql",
        "db/mysql/04-workflow.sql",
        "db/mysql/05-business.sql",
        "db/mysql/06-tool.sql",
        "db/mysql/07-quartz.sql"
);
```

The test must load UTF-8 resources, extract table names with:

```java
Pattern.compile(
        "(?i)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?([A-Za-z0-9_]+)`?"
);
```

Assertions:

```java
assertEquals(moduleTables, tables("db/mysql/heartbeat-all.sql"));
assertEquals(moduleTables, tables("db/schema-mysql.sql"));
assertTrue(tables("schema.sql").containsAll(applicationTables(moduleTables)));
assertTrue(resource("db/mysql/90-seed.sql").contains("INSERT"));
assertFalse(resource("db/mysql/heartbeat-all.sql").contains("SOURCE "));
assertFalse(resource("db/mysql/heartbeat-all.sql").contains("鐢熶骇"));
```

Exclude Quartz `QRTZ_*` tables when comparing against H2 because local profile uses memory JobStore.

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=DatabaseSqlOrganizationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because `db/mysql/*.sql` does not exist.

### Task 2: Create environment database bootstrap SQL

**Files:**
- Create: `heartbeat-start/src/main/resources/db/mysql/00-create-databases.sql`

- [ ] **Step 1: Add idempotent database creation**

Create:

```sql
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `heartbeat_dev`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `heartbeat_test`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `heartbeat_pre`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `heartbeat_gray`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `heartbeat`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

Do not create users or embed passwords.

### Task 3: Split application DDL by bounded context

**Files:**
- Create: `heartbeat-start/src/main/resources/db/mysql/01-structure.sql`
- Create: `heartbeat-start/src/main/resources/db/mysql/02-platform.sql`
- Create: `heartbeat-start/src/main/resources/db/mysql/03-flow.sql`
- Create: `heartbeat-start/src/main/resources/db/mysql/04-workflow.sql`
- Create: `heartbeat-start/src/main/resources/db/mysql/05-business.sql`
- Create: `heartbeat-start/src/main/resources/db/mysql/06-tool.sql`

- [ ] **Step 1: Add structure intelligence tables**

`01-structure.sql` must create:

```text
structure_definition
structure_version
structure_publish_audit
```

Keep immutable version uniqueness on `(definition_id, version_no)` and index publish audit by `(definition_id, version_no, created_at)`.

- [ ] **Step 2: Add platform governance tables**

`02-platform.sql` must create:

```text
users
sys_resource_base
sys_tenant
sys_tenant_package
sys_tenant_feature
sys_dept
sys_post
sys_role
sys_api_permission
sys_dict_type
sys_dict_data
sys_config
sys_notice
sys_oper_log
sys_login_log
sys_online_session
sys_oauth_client
sys_social_provider
sys_user_social_bind
sys_sso_session
sys_user
sys_menu
sys_user_preference
sys_role_menu
sys_user_role
sys_role_dept
hb_social_provider
hb_social_bind
```

Create each resource table explicitly through `CREATE TABLE ... LIKE sys_resource_base`; keep relation tables on composite primary keys.

- [ ] **Step 3: Add automation flow tables**

`03-flow.sql` must create:

```text
hb_node_component
hb_flow_definition
hb_flow_version
hb_connection_credential
hb_flow_run
hb_flow_run_event
```

Preserve the current MyBatis-Flex entity column names and add indexes for component type/version, flow code, flow version, run history, and run events.

- [ ] **Step 4: Add human workflow tables**

`04-workflow.sql` must create:

```text
wf_process_definition
wf_process_instance
wf_task
wf_task_action
```

Index definitions by `(tenant_id, definition_key, version_no)`, instances by `(tenant_id, definition_id, status)`, tasks by `(tenant_id, assignee_id, status, created_at)`, and actions by `(tenant_id, task_id, created_at)`.

- [ ] **Step 5: Add business extension tables**

`05-business.sql` must create:

```text
pay_channel
pay_order
pay_notify_log
mp_account
mp_menu
mp_material
mp_auto_reply
report_dataset
report_template
report_query_log
mobile_app
mobile_page
mobile_api_route
```

Tenant-scope business unique keys such as payment order number, dataset key, template key, application key, page key, and API route key.

- [ ] **Step 6: Add code generation and scheduler resource tables**

`06-tool.sql` must create:

```text
gen_table
gen_table_column
sys_job
sys_job_log
```

Use the same `PlatformResource` column contract expected by `PlatformFlexResourceSupport`.

### Task 4: Add Quartz and seed scripts

**Files:**
- Create: `heartbeat-start/src/main/resources/db/mysql/07-quartz.sql`
- Create: `heartbeat-start/src/main/resources/db/mysql/90-seed.sql`

- [ ] **Step 1: Normalize Quartz DDL**

Copy the current MySQL Quartz table definitions from `db/heartbeat_quartz.sql`, preserve the configured `QRTZ_` prefix, and rewrite comments/header as valid UTF-8.

- [ ] **Step 2: Consolidate seed data**

`90-seed.sql` must initialize:

```text
default tenant 1
default department 1
super_admin role 1
admin user 1
sys_user_role relation
system configuration
demo Quartz job
MOCK social provider
all current DIR/MENU/BUTTON permissions
all role-menu relations for role 1
```

Use idempotent inserts. Do not seed payment secrets, OAuth secrets, MQ credentials, database credentials, or real third-party application secrets.

### Task 5: Build aggregate and compatibility scripts

**Files:**
- Create: `heartbeat-start/src/main/resources/db/mysql/heartbeat-all.sql`
- Modify: `heartbeat-start/src/main/resources/db/schema-mysql.sql`
- Modify: `heartbeat-start/src/main/resources/db/heartbeat_sys.sql`
- Modify: `heartbeat-start/src/main/resources/db/heartbeat_quartz.sql`

- [ ] **Step 1: Materialize the aggregate installer**

Concatenate the actual SQL bodies in this order:

```text
session preamble
01-structure.sql
02-platform.sql
03-flow.sql
04-workflow.sql
05-business.sql
06-tool.sql
07-quartz.sql
90-seed.sql
session epilogue
```

Do not use `SOURCE`.

- [ ] **Step 2: Synchronize compatibility entry points**

Make `db/schema-mysql.sql` byte-equivalent to `db/mysql/heartbeat-all.sql` except for its compatibility header.

Make `db/heartbeat_sys.sql` contain all non-Quartz application DDL plus seed data.

Make `db/heartbeat_quartz.sql` contain the same Quartz DDL as `db/mysql/07-quartz.sql`.

- [ ] **Step 3: Run the SQL organization test**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=DatabaseSqlOrganizationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS.

### Task 6: Synchronize H2 local schema

**Files:**
- Modify: `heartbeat-start/src/main/resources/schema.sql`

- [ ] **Step 1: Align application table contracts**

Ensure H2 contains every non-Quartz application table from the MySQL module scripts with matching:

```text
table names
column names
primary keys
business unique constraints
required indexes used by repositories
```

Use H2-compatible explicit `CREATE TABLE` statements and avoid MySQL `LIKE`, session variables, and `ON UPDATE`.

- [ ] **Step 2: Verify local startup**

Run:

```powershell
java -jar heartbeat-start\target\heartbeat.jar --spring.profiles.active=local --spring.main.banner-mode=off
```

Expected log:

```text
Tomcat started on port(s): 7001
Started HeartBeatApplication
```

Stop the verification process after confirming startup.

### Task 7: Document installation and verify the full change

**Files:**
- Modify: `README.md`
- Modify: `docs/saas-admin-mvp.md`

- [ ] **Step 1: Document SQL layout and commands**

Document:

```text
dev  -> heartbeat_dev
test -> heartbeat_test
pre  -> heartbeat_pre
gray -> heartbeat_gray
prod -> heartbeat
```

Add Windows PowerShell examples for running `00-create-databases.sql` and `heartbeat-all.sql`, plus the `local` H2 alternative.

- [ ] **Step 2: Run focused verification**

Run:

```powershell
mvn --% -pl heartbeat-start -am -Dtest=DatabaseSqlOrganizationTest,EnvironmentConfigurationTest,MybatisFlexOnlyPersistenceTest,TransactionManagerConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all selected tests PASS.

- [ ] **Step 3: Package the backend**

Run:

```powershell
mvn --% -pl heartbeat-start -am -DskipTests package
```

Expected: `BUILD SUCCESS` and `heartbeat-start/target/heartbeat.jar`.

- [ ] **Step 4: Scan final scripts**

Run a repository scan confirming:

```text
no mojibake marker "鐢熶骇"
no real credential values
no SOURCE directives in heartbeat-all.sql
module table set equals aggregate table set
H2 contains all non-Quartz tables
```

No commit step is included because the workspace does not contain a valid Git repository.

