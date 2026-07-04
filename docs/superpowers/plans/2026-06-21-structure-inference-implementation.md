# Structure Inference Full-Stack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot API that infers a reusable structure model from multiple JSON samples, generates JSON Schema/UI Schema, versions saved definitions, validates payloads, and exposes a simple React UI.

**Architecture:** The backend adds a `structure` context across the existing DDD modules. JSON is parsed in the application layer, merged into a format-neutral domain model, passed through registered artifact generators, and persisted as immutable versions through an infrastructure repository. A Vite/React app consumes the REST API for preview, save, version activation, and validation.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Jackson, Spring Data JPA, H2/MySQL, JUnit 5, React 18, Vite 5, Vitest.

---

## File map

### Backend core

- Create `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/structure/model/StructureType.java`
- Create `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/structure/model/StructureNode.java`
- Create `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/structure/model/InferenceWarning.java`
- Create `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/structure/model/StructureDefinition.java`
- Create `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/structure/model/StructureVersion.java`
- Create `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/structure/repository/StructureDefinitionRepository.java`
- Create `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/structure/StructureErrorCode.java`

### Backend application

- Create `heartbeat-application/src/main/java/top/kx/heartbeat/application/structure/StructureInferenceEngine.java`
- Create `heartbeat-application/src/main/java/top/kx/heartbeat/application/structure/StructureApplicationService.java`
- Create `heartbeat-application/src/main/java/top/kx/heartbeat/application/structure/artifact/ArtifactGenerator.java`
- Create `heartbeat-application/src/main/java/top/kx/heartbeat/application/structure/artifact/ArtifactGeneratorRegistry.java`
- Create `heartbeat-application/src/main/java/top/kx/heartbeat/application/structure/artifact/JsonSchemaGenerator.java`
- Create `heartbeat-application/src/main/java/top/kx/heartbeat/application/structure/artifact/UiSchemaGenerator.java`
- Create DTO/command classes under `heartbeat-application/src/main/java/top/kx/heartbeat/application/structure/dto`
- Modify `heartbeat-application/pom.xml`

### Backend persistence and API

- Create JPA entities/repositories/converters under `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/structure`
- Create REST controller/request/response classes under `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/structure`
- Modify `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/common/GlobalExceptionHandler.java`
- Modify `heartbeat-start/src/main/resources/db/schema-mysql.sql`

### React

- Create `heartbeat-web/package.json`
- Create `heartbeat-web/vite.config.js`
- Create `heartbeat-web/index.html`
- Create `heartbeat-web/src/main.jsx`
- Create `heartbeat-web/src/App.jsx`
- Create `heartbeat-web/src/api.js`
- Create `heartbeat-web/src/styles.css`
- Create tests under `heartbeat-web/src/*.test.jsx`

## Task 1: Multi-sample inference core

- [ ] Write `StructureInferenceEngineTest` first with cases for optional fields, nullable values, numeric widening, nested objects, object arrays, mixed types, and empty-array warnings.
- [ ] Run:

```powershell
mvn -pl heartbeat-application -am -Dtest=StructureInferenceEngineTest test
```

Expected: fail because the structure model and inference engine do not exist.

- [ ] Implement the format-neutral domain types and minimal inference engine.
- [ ] Re-run the focused test and confirm all cases pass.

The public inference API is:

```java
InferenceResult infer(List<JsonNode> samples);
```

`InferenceResult` contains `StructureNode root` and `List<InferenceWarning> warnings`.

## Task 2: Artifact generators and payload validation

- [ ] Write failing tests for:
  - JSON Schema Draft 2020-12 output.
  - Required/optional fields.
  - `additionalProperties=true` in LENIENT mode.
  - `additionalProperties=false` in STRICT mode.
  - UI widget defaults and override support.
  - JSON-path validation errors.
- [ ] Run:

```powershell
mvn -pl heartbeat-application -am -Dtest=JsonSchemaGeneratorTest,UiSchemaGeneratorTest,StructurePayloadValidatorTest test
```

Expected: fail because generators and validator are missing.

- [ ] Implement:

```java
public interface ArtifactGenerator {
    String artifactType();
    JsonNode generate(StructureNode model, GenerationOptions options);
}
```

- [ ] Implement a registry that rejects duplicate artifact types and reports unsupported types.
- [ ] Implement JSON Schema, UI Schema, and direct model-based validation.
- [ ] Re-run focused tests.

## Task 3: Versioned structure definitions

- [ ] Write failing application tests using an in-memory test repository for:
  - Preview does not persist.
  - Create definition creates version 1.
  - New version increments the number.
  - Activation and rollback change only the active pointer.
  - Validation defaults to the active version.
- [ ] Run:

```powershell
mvn -pl heartbeat-application -am -Dtest=StructureApplicationServiceTest test
```

Expected: fail because the application service and repository contract are missing.

- [ ] Implement immutable `StructureVersion`, `StructureDefinition`, repository contract, commands, DTOs, and application service.
- [ ] Store sample SHA-256 digest instead of raw samples.
- [ ] Re-run focused tests.

## Task 4: JPA persistence and REST API

- [ ] Write failing Spring integration tests covering:
  - `POST /api/v1/structure-definitions/preview`
  - `POST /api/v1/structure-definitions`
  - list/detail/version endpoints
  - active-version update
  - validation in LENIENT and STRICT modes
- [ ] Run:

```powershell
mvn -pl heartbeat-start -am -Dtest=StructureDefinitionApiTest test
```

Expected: fail because the controller and persistence adapter are absent.

- [ ] Implement JPA entities with JSON stored as `@Lob` text, converters, repositories, REST request/response mapping, and stable structure error codes.
- [ ] Add MySQL DDL for `structure_definition` and `structure_version`.
- [ ] Re-run focused integration tests.

The first implementation exposes:

```text
POST /api/v1/structure-definitions/preview
POST /api/v1/structure-definitions
GET  /api/v1/structure-definitions
GET  /api/v1/structure-definitions/{id}
POST /api/v1/structure-definitions/{id}/versions
GET  /api/v1/structure-definitions/{id}/versions
PUT  /api/v1/structure-definitions/{id}/active-version
POST /api/v1/structure-definitions/{id}/validate
```

## Task 5: React user interface

- [ ] Create a failing Vitest test proving that clicking “预览生成” sends parsed samples and renders JSON Schema.
- [ ] Run:

```powershell
cd heartbeat-web
npm.cmd test -- --run
```

Expected: fail because the application is missing.

- [ ] Implement a single-page interface with:
  - Definition name.
  - JSON array sample editor.
  - LENIENT/STRICT selector.
  - Preview action.
  - Warning list.
  - JSON Schema/UI Schema tabs.
  - Save definition action.
  - Saved definition/version list.
  - Payload validation panel.
- [ ] Configure Vite proxy `/api` to `http://localhost:8080`.
- [ ] Add responsive styling without introducing a component library.
- [ ] Re-run frontend tests.

## Task 6: End-to-end verification

- [ ] Run all backend tests:

```powershell
mvn clean test
```

- [ ] Run frontend tests and production build:

```powershell
cd heartbeat-web
npm.cmd test -- --run
npm.cmd run build
```

- [ ] Start Spring Boot and verify preview/save/validate using HTTP requests.
- [ ] Start Vite and verify the React page loads, previews schemas, saves a version, and validates a sample.
- [ ] Update `README.md` with backend and frontend startup commands.

