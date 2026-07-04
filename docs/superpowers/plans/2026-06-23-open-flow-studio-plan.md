# Open Flow Studio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build HeartBeat's own extensible visual flow platform: a canvas-based workflow designer where every node comes from an open component library. The platform should support data sources such as MySQL, Redis, MQ, HTTP, Webhook, and internal services without coupling the canvas to any single business rule model.

**Architecture:** Treat the canvas as a renderer/editor for a neutral Flow DSL. Treat the component library as a registry of node manifests, ports, configuration schemas, credentials, and runtime executor identifiers. The backend owns component metadata, flow versioning, publishing, credential isolation, compilation, execution, and run observability. The frontend owns palette browsing, node placement, connection editing, schema-driven node configuration, debugging, and visual execution traces.

**Tech Stack:** Spring Boot 2.7, Java 8, Jackson, JdbcTemplate/MySQL/H2, React 18, Vite 5, React Flow or an adapter-ready canvas abstraction, Vitest, Testing Library, JUnit 5, MockMvc.

---

## Design Principles

- Do not copy ThingLinks' rule or warning model. Only borrow the product pattern of "component palette + canvas + property panel + execution/debug feedback".
- The canvas must not know what MySQL, MQ, Redis, or HTTP means. It only reads component manifests, node config, ports, and edges.
- The Flow DSL is the stable contract. UI library choices such as React Flow, AntV X6, or LogicFlow must be replaceable behind adapters.
- Component definitions should be open: built-in components first, database-registered components next, remote/plugin manifests later.
- Credentials must be separate from flow JSON. Flow nodes reference `connectionId` or credential aliases, never raw passwords.
- Runtime execution should support both short synchronous test runs and later asynchronous production runs with retries, logs, and tracing.

## Reference Projects To Study

- **Node-RED:** node palette, flow JSON, node config forms, debug node, context model, community node packaging.
- **n8n:** credential management, expression language, node input/output preview, execution history, workflow templates.
- **Apache NiFi:** processor model, connection queues, back pressure, provenance/event tracing, runtime status.
- **React Flow:** React-native canvas implementation, custom nodes, handles, edges, minimap, selection, viewport controls.
- **AntV X6:** enterprise graph editing patterns, ports, embedding, stencil/palette, graph serialization.
- **LogicFlow:** BPMN-like process editing, plugins, flow validation, process designer ergonomics.
- **Temporal / Camunda / Zeebe:** reliable long-running workflows, replay, human tasks, workflow state machines.
- **Airflow / Kestra / Dagster:** DAG compilation, scheduling, retries, task observability, environment-aware deployment.

---

### Task 1: Flow DSL and component manifest specification

**Files:**
- Create: `docs/superpowers/specs/2026-06-23-open-flow-studio-design.md`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/model/FlowDefinition.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/model/FlowNode.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/model/FlowEdge.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/model/NodeComponentManifest.java`
- Test: `heartbeat-domain/src/test/java/top/kx/heartbeat/domain/flow/FlowDslTest.java`

- [ ] Document the neutral Flow DSL: `id`, `name`, `version`, `nodes`, `edges`, `variables`, `settings`, and metadata.
- [ ] Document the node component manifest: `type`, `category`, `version`, `ports`, `configSchema`, `runtime`, `capabilities`, and validation rules.
- [ ] Define port semantics for `input`, `output`, `true`, `false`, `error`, and dynamic ports.
- [ ] Define config schema compatibility with JSON Schema so the frontend can render node forms dynamically.
- [ ] Add failing domain tests for DSL parsing, duplicate node IDs, missing edge endpoints, invalid port references, and unsupported manifest versions.
- [ ] Implement minimal domain model and validators.
- [ ] Re-run focused domain tests and confirm they pass.

### Task 2: Component registry and built-in node library

**Files:**
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/repository/NodeComponentRepository.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/NodeComponentRegistryService.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/NodeComponentJdbcRepository.java`
- Modify: `heartbeat-start/src/main/resources/db/schema-mysql.sql`
- Test: `heartbeat-application/src/test/java/top/kx/heartbeat/application/flow/NodeComponentRegistryServiceTest.java`

- [ ] Add failing tests for listing components by category, resolving component by type/version, rejecting duplicate active manifests, and filtering disabled components.
- [ ] Create `hb_node_component` with manifest JSON, category, type, version, status, source, and audit fields.
- [ ] Seed first-party components: manual trigger, webhook trigger, MySQL query, Redis get/set, MQ publish, HTTP request, condition, field mapper, log, and end.
- [ ] Keep executor IDs declarative, for example `builtin:mysql.query`, `builtin:redis.get`, `builtin:logic.condition`.
- [ ] Add registry APIs that return frontend-safe manifests without secret values.
- [ ] Re-run component registry tests.

### Task 3: Flow definition, versioning, publishing, and rollback

**Files:**
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/repository/FlowRepository.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/FlowApplicationService.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/FlowJdbcRepository.java`
- Create: `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/flow/FlowController.java`
- Modify: `heartbeat-start/src/main/resources/db/schema-mysql.sql`
- Test: `heartbeat-application/src/test/java/top/kx/heartbeat/application/flow/FlowApplicationServiceTest.java`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/flow/FlowApiTest.java`

- [ ] Add failing service tests for draft creation, draft update, version creation, active version switching, rollback, and immutable published versions.
- [ ] Create `hb_flow_definition` and `hb_flow_version`; store full Flow DSL per version.
- [ ] Compile and validate DSL before publishing a version.
- [ ] Add API endpoints for create, update draft, preview compile, publish, list versions, activate version, rollback, and delete/archive.
- [ ] Wire menu permission names around `flow:definition:*` without depending on legacy rule concepts.
- [ ] Re-run application and API tests.

### Task 4: Credential and connection management

**Files:**
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/model/ConnectionCredential.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/repository/ConnectionCredentialRepository.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/ConnectionCredentialService.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/ConnectionCredentialJdbcRepository.java`
- Create: `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/flow/ConnectionCredentialController.java`
- Modify: `heartbeat-start/src/main/resources/db/schema-mysql.sql`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/flow/ConnectionCredentialApiTest.java`

- [ ] Add failing tests for creating MySQL/Redis/MQ/HTTP credentials, masking secrets on read, rotating secrets, and rejecting raw secret exposure.
- [ ] Store connection metadata separately from encrypted secret material.
- [ ] Let node configs reference `connectionId` and non-secret parameters only.
- [ ] Add "test connection" API using executor-specific probes.
- [ ] Add tenant/user ownership checks if the existing auth model supports it.
- [ ] Re-run credential API tests.

### Task 5: Flow compiler and local debug runtime

**Files:**
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowCompiler.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowExecutor.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/NodeExecutor.java`
- Create: `heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/BuiltinNodeExecutors.java`
- Test: `heartbeat-application/src/test/java/top/kx/heartbeat/application/flow/runtime/FlowCompilerTest.java`
- Test: `heartbeat-application/src/test/java/top/kx/heartbeat/application/flow/runtime/FlowExecutorTest.java`

- [ ] Add failing compiler tests for cycles, unreachable nodes, invalid source nodes, invalid sink nodes, and type/port mismatches.
- [ ] Add failing executor tests for manual payload input, condition branching, mapper output, log node, and error port routing.
- [ ] Implement DAG compilation from Flow DSL.
- [ ] Implement synchronous debug execution with per-node input, output, status, elapsed time, and error details.
- [ ] Keep external components mocked in unit tests; real MySQL/Redis/MQ execution can be added behind integration tests later.
- [ ] Re-run runtime tests.

### Task 6: React Flow Studio page and canvas adapter

**Files:**
- Create: `heartbeat-web/src/pages/flow/FlowStudioPage.jsx`
- Create: `heartbeat-web/src/components/flow/FlowPalette.jsx`
- Create: `heartbeat-web/src/components/flow/FlowCanvas.jsx`
- Create: `heartbeat-web/src/components/flow/FlowNode.jsx`
- Create: `heartbeat-web/src/components/flow/FlowInspector.jsx`
- Create: `heartbeat-web/src/components/flow/FlowDebugPanel.jsx`
- Create: `heartbeat-web/src/domain/flow/flowDsl.js`
- Create: `heartbeat-web/src/domain/flow/componentRegistry.js`
- Modify: `heartbeat-web/src/api.js`
- Modify: `heartbeat-web/src/App.jsx`
- Modify: `heartbeat-web/src/styles.css`
- Test: `heartbeat-web/src/pages/flow/FlowStudioPage.test.jsx`

- [ ] Add `reactflow` dependency or choose a canvas adapter library after a short spike.
- [ ] Add failing tests for palette rendering, dragging/adding a node, selecting a node, editing config, connecting valid ports, rejecting invalid connections, and serializing DSL.
- [ ] Implement a canvas adapter boundary so future X6/LogicFlow migration does not change Flow DSL.
- [ ] Render palette categories from component manifests returned by the registry API.
- [ ] Render node configuration forms from `configSchema`.
- [ ] Add a JSON DSL preview panel for development and debugging.
- [ ] Re-run frontend tests and build.

### Task 7: Execution history and observability

**Files:**
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/model/FlowRun.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/model/FlowRunEvent.java`
- Create: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/flow/repository/FlowRunRepository.java`
- Create: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/flow/FlowRunJdbcRepository.java`
- Modify: `heartbeat-application/src/main/java/top/kx/heartbeat/application/flow/runtime/FlowExecutor.java`
- Modify: `heartbeat-web/src/components/flow/FlowDebugPanel.jsx`
- Modify: `heartbeat-start/src/main/resources/db/schema-mysql.sql`
- Test: `heartbeat-application/src/test/java/top/kx/heartbeat/application/flow/runtime/FlowRunHistoryTest.java`

- [ ] Add failing tests for recording run status, node event order, node input/output summaries, error stack truncation, and elapsed time.
- [ ] Create `hb_flow_run` and `hb_flow_run_event`.
- [ ] Persist debug runs and later production runs through the same event model.
- [ ] Add frontend execution trace view with selected-node input/output inspection.
- [ ] Add filters for success, failed, running, and canceled runs.
- [ ] Re-run runtime and frontend tests.

### Task 8: Extension model and derived product capabilities

**Files:**
- Create: `docs/superpowers/specs/2026-06-23-flow-extension-model.md`
- Create: `docs/superpowers/specs/2026-06-23-flow-derived-features.md`

- [ ] Define extension phases: built-in manifests, database-registered manifests, uploaded plugin bundles, remote marketplace manifests.
- [ ] Define plugin safety rules: allowed executor IDs, sandboxing strategy, network permission, credential scope, version compatibility, and audit logging.
- [ ] Document derived products: data sync pipelines, IoT automation, low-code automation, form linkage, alert routing, ETL, workflow templates, AI-generated flows, and integration marketplace.
- [ ] Document compatibility strategy for moving from local debug runtime to queued/asynchronous execution.
- [ ] Document a future expression language inspired by n8n, JSONata, CEL, or SpEL, with a preference for deterministic and sandboxable evaluation.

## MVP Scope

- Open component registry with database-backed manifests.
- Flow DSL save/load/version/publish.
- React canvas with palette, node config panel, port connection, and JSON preview.
- Local debug execution for manual trigger, condition, mapper, log, HTTP mock, Redis mock, MQ mock, and end nodes.
- Credential references and secret masking.
- Execution trace with node input/output and error details.

## Out Of Scope For MVP

- Full distributed runtime.
- User-uploaded executable plugins.
- BPMN approval workflows.
- Long-running workflow replay.
- Complex back pressure and queue management.
- Multi-environment deployment promotion.
- Public marketplace.

## Self-review

- This plan defines HeartBeat's own flow platform and does not reuse ThingLinks' rule engine.
- The component library is the core abstraction and every canvas node comes from a manifest.
- MySQL, Redis, MQ, HTTP, Webhook, logic, transform, and system nodes are modeled as extensible components.
- The Flow DSL remains independent from the chosen frontend graph library.
- Security-sensitive credentials are isolated from flow JSON.
- The plan leaves room for Node-RED, n8n, NiFi, Temporal, Camunda, and DAG-oriented systems as references without copying their implementation.
