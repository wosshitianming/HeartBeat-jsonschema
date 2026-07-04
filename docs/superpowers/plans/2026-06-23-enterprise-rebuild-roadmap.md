# HeartBeat Enterprise Rebuild Roadmap

> **For agentic workers:** Each phase has its own implementation plan and must leave the application buildable and testable. Do not start a later phase while an earlier phase has failing contract, tenant-isolation, or compatibility tests.

**Goal:** Replace the generic resource persistence model with dedicated enterprise-grade MySQL schemas, typed domain repositories, trusted tenant isolation, and compatible APIs.

**Architecture:** The rebuild is delivered as a sequence of bounded-context migrations. MySQL/Flyway is the production contract; MyBatis-Flex provides typed persistence; existing front-end routes are retained through compatibility adapters until the web application moves to the new APIs.

**Tech Stack:** Java 8, Spring Boot 2.7.18, MyBatis-Flex 1.11.7, MySQL 8.0, Flyway, Spring Security, Redis, Quartz, RabbitMQ/Kafka, JUnit 5, Testcontainers.

---

## Phase order

| Phase | Scope | Exit condition |
| --- | --- | --- |
| 1 | Database foundation, tenant, IAM, authentication, configuration, audit, compatibility APIs | Login, users, departments, posts, roles, permissions, menus, dictionaries, configuration, notices, social login and audit run without `PlatformResource` |
| 2 | Structure intelligence | Drafts and immutable versions use dedicated tables; Schema/UI Schema/artifacts are independently persisted |
| 3 | Automation, approval and reliable events | Flow waits for approval and resumes through Outbox/Inbox with idempotency |
| 4 | Payment | Channels, orders, transactions, refunds and notifications use typed state machines and idempotency |
| 5 | Official account, reports and mobile | Each domain has typed repositories; report SQL is parsed, parameterized and audited |
| 6 | Code generation and scheduling | Metadata is column-based; jobs use registered handlers instead of arbitrary reflection |
| 7 | Final cleanup and production hardening | Generic resource code, large generic repository, legacy SQL bundles and untrusted tenant headers are gone |

## Global gates

Every phase must satisfy:

- Maven compilation succeeds.
- Focused domain tests pass.
- Cross-tenant read/update/delete tests pass for every new tenant table.
- Existing front-end compatibility routes used by that phase continue to pass.
- No new repository returns `Map<String, Object>`.
- No new table copies another table’s complete business shape.
- No stable business field is hidden in a generic `payload`.
- No plaintext password or secret is returned or logged.

## Plan documents

- Phase 1: `docs/superpowers/plans/2026-06-23-enterprise-rebuild-phase-1-foundation-iam-auth.md`
- Remaining phase plans are written only after Phase 1 verification, using the same design specification:
  `docs/superpowers/specs/2026-06-23-enterprise-database-rebuild-design.md`

