# Pure DDD Boundary Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move generic admin/read-write ports out of `heartbeat-domain`, keep domain repositories focused on domain models, and align infrastructure adapter naming.

**Architecture:** Command-side domain repositories remain in `heartbeat-domain` only when they return domain models and enforce aggregate boundaries. Generic `DomainRecord`/`Map` ports move to `heartbeat-application` as transitional `Repository` ports or read-model ports. Infrastructure implements both application repositories and true domain repositories through `XxxRepositoryImpl` adapters.

**Tech Stack:** Java 8, Spring Boot 2.7, MyBatis Generator `Example/Criteria`, Maven multi-module project.

---

### Task 1: Move Generic Record Type To Application

**Files:**
- Move: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/common/model/DomainRecord.java`
- To: `heartbeat-application/src/main/java/top/kx/heartbeat/application/common/model/DomainRecord.java`
- Modify imports in application and infrastructure files that currently use `top.kx.heartbeat.domain.common.model.DomainRecord`.

- [x] **Step 1: Move `DomainRecord` package**

Change package from:

```java
package top.kx.heartbeat.domain.common.model;
```

to:

```java
package top.kx.heartbeat.application.common.model;
```

- [x] **Step 2: Update imports**

Replace:

```java
import top.kx.heartbeat.domain.common.model.DomainRecord;
```

with:

```java
import top.kx.heartbeat.application.common.model.DomainRecord;
```

in production code.

### Task 2: Move Transitional Repositories Out Of Domain

**Files:**
- Move platform, mobile, mp, pay, report, workflow, and codegen preview ports from `heartbeat-domain` into `heartbeat-application/**/port`.
- Update application services and infrastructure adapters to import the new port packages.

- [x] **Step 1: Move platform administration port**

Move:

```text
heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/PlatformAdministrationRepository.java
```

to:

```text
heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/port/PlatformAdministrationRepository.java
```

and change package to:

```java
package top.kx.heartbeat.application.platform.port;
```

- [x] **Step 2: Move other generic ports**

Move these ports and change packages:

```text
domain/mobile/MobileRepository.java -> application/mobile/port/MobileRepository.java
domain/mp/MpRepository.java -> application/mp/port/MpRepository.java
domain/mp/MpMenuSyncGateway.java -> application/mp/port/MpMenuSyncGateway.java
domain/pay/PayRepository.java -> application/pay/port/PayRepository.java
domain/report/ReportRepository.java -> application/report/port/ReportRepository.java
domain/workflow/WorkflowRepository.java -> application/workflow/port/WorkflowRepository.java
domain/tool/MybatisGeneratorPreviewer.java -> application/tool/port/MybatisGeneratorPreviewer.java
```

- [x] **Step 3: Update imports**

Update service and adapter imports from `top.kx.heartbeat.domain.<context>...` to the new `top.kx.heartbeat.application.<context>.port...` packages.

### Task 3: Allow Infrastructure To Implement Application Ports

**Files:**
- Modify: `heartbeat-infrastructure/pom.xml`

- [x] **Step 1: Add application dependency**

Add:

```xml
<dependency>
    <groupId>top.kx</groupId>
    <artifactId>heartbeat-application</artifactId>
</dependency>
```

next to the existing `heartbeat-domain` dependency.

### Task 4: Rename Infrastructure Adapters

**Files:**
- Move and edit:
  - `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/PlatformAdministrationMybatisRepository.java`
  - `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/auth/AuthSessionMybatisRepository.java`
  - `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/mobile/MobileMybatisRepository.java`
  - `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/report/ReportMybatisRepository.java`
  - `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/workflow/WorkflowMybatisRepository.java`

- [x] **Step 1: Rename classes and packages**

Target names:

```text
infrastructure/platform/repository/PlatformAdministrationRepositoryImpl.java
infrastructure/auth/repository/AuthSessionRepositoryImpl.java
infrastructure/mobile/repository/MobileRepositoryImpl.java
infrastructure/mp/repository/MpRepositoryImpl.java
infrastructure/pay/repository/PayRepositoryImpl.java
infrastructure/report/repository/ReportRepositoryImpl.java
infrastructure/workflow/repository/WorkflowRepositoryImpl.java
```

Each class should use package:

```java
package top.kx.heartbeat.infrastructure.<context>.repository;
```

- [x] **Step 2: Update imports**

Update implementation imports to use application repositories for transitional ports and domain ports for true domain repositories.

### Task 5: Source Guard Checks

**Files:** No production file edits.

- [x] **Step 1: Check domain no longer owns generic ports**

Run:

```powershell
rg -n "DomainRecord|Map<String, Object>|PlatformAdministrationRepository|MobileRepository|MpRepository|PayRepository|ReportRepository|WorkflowRepository|MybatisGeneratorPreviewer" heartbeat-domain/src/main/java/top/kx/heartbeat/domain
```

Expected: only meaningful domain model JSON fields and domain enums/models remain; no generic repository port files remain.

- [x] **Step 2: Check old MyBatis repository names**

Run:

```powershell
rg -n "class .*MybatisRepository|MybatisRepository" heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure
```

Expected: no renamed adapter classes remain. Older non-target repositories can be handled in later phases if discovered.

### Task 6: Deferred Compile

Compilation is intentionally deferred because the user asked to modify code first and run build later in one batch. When ready, run:

```powershell
mvn --% -pl heartbeat-start -am -DskipTests compile
```
