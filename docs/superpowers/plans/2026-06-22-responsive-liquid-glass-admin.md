# Responsive Liquid Glass Admin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a stable responsive admin interface with three user-specific liquid-glass themes, backend preference synchronization, and a full mobile CRUD workflow.

**Architecture:** Add a narrow appearance-preference API to the existing admin platform service and repository. On the frontend, isolate theme persistence in a hook/service, isolate the switcher and mobile shell into focused components, and keep the existing domain/API operations as the shared data layer for desktop and mobile presentations.

**Tech Stack:** Spring Boot 2.7, JdbcTemplate, MySQL/H2, React 18, CSS custom properties, Vitest, Testing Library, JUnit 5, MockMvc.

---

### Task 1: Appearance preference API

**Files:**
- Modify: `heartbeat-domain/src/main/java/top/kx/heartbeat/domain/platform/AdminPlatformRepository.java`
- Modify: `heartbeat-application/src/main/java/top/kx/heartbeat/application/platform/AdminPlatformService.java`
- Modify: `heartbeat-infrastructure/src/main/java/top/kx/heartbeat/infrastructure/platform/AdminPlatformJdbcRepository.java`
- Modify: `heartbeat-interfaces/src/main/java/top/kx/heartbeat/interfaces/auth/AuthController.java`
- Modify: `heartbeat-start/src/main/resources/db/schema-mysql.sql`
- Test: `heartbeat-start/src/test/java/top/kx/heartbeat/admin/AdminPlatformApiTest.java`

- [ ] Add a failing MockMvc test that reads the default `balanced` theme, writes `immersive`, reads it back, and rejects an unsupported value.
- [ ] Run `mvn -pl heartbeat-start -am -Dtest=AdminPlatformApiTest -Dsurefire.failIfNoSpecifiedTests=false test` and confirm the new assertions fail.
- [ ] Add repository methods `findUserPreference` and `saveUserPreference`, create `sys_user_preference` in both runtime initialization and MySQL schema, and map `updateTime`.
- [ ] Add service validation for `immersive`, `balanced`, and `professional`, returning `balanced` when no value exists.
- [ ] Add `GET` and `PUT /api/v1/auth/preferences/appearance`.
- [ ] Re-run the focused Maven test and confirm it passes.

### Task 2: Frontend theme state and synchronization

**Files:**
- Create: `heartbeat-web/src/appearance/themeService.js`
- Create: `heartbeat-web/src/appearance/useAppearanceTheme.js`
- Create: `heartbeat-web/src/components/admin/ThemeSwitcher.jsx`
- Modify: `heartbeat-web/src/api.js`
- Modify: `heartbeat-web/src/App.jsx`
- Test: `heartbeat-web/src/appearance/themeService.test.js`
- Test: `heartbeat-web/src/App.test.jsx`

- [ ] Add failing tests for validation, per-user local-storage keys, immediate document theme application, backend reconciliation, and PUT synchronization.
- [ ] Run `npm.cmd test -- --run` in `heartbeat-web` and confirm the new tests fail.
- [ ] Implement the three theme constants, local persistence helpers, document dataset application, and the appearance API client.
- [ ] Implement the hook so local state applies immediately, backend state reconciles after authentication, and failed saves remain locally usable with a pending-sync indicator.
- [ ] Add an accessible theme switcher to the top toolbar.
- [ ] Re-run frontend tests and confirm they pass.

### Task 3: Desktop admin visual restructuring

**Files:**
- Create: `heartbeat-web/src/components/admin/AdminSidebar.jsx`
- Create: `heartbeat-web/src/components/admin/AdminTopbar.jsx`
- Create: `heartbeat-web/src/components/admin/ResourceTable.jsx`
- Modify: `heartbeat-web/src/App.jsx`
- Modify: `heartbeat-web/src/styles.css`
- Test: `heartbeat-web/src/App.test.jsx`

- [ ] Add failing render tests for grouped navigation, compact page heading, theme switcher, resource table semantics, and stable action labels.
- [ ] Replace the presentation hero with a compact page header and standard toolbar/table hierarchy.
- [ ] Define CSS variables for color, opacity, blur, border, shadow, radius, density, and motion under the three `data-theme` values.
- [ ] Implement desktop breakpoints for expanded, compact, and icon sidebars without changing CRUD behavior.
- [ ] Run frontend tests and build; confirm both pass.

### Task 4: Mobile App-style full CRUD workflow

**Files:**
- Create: `heartbeat-web/src/components/admin/MobileAdminShell.jsx`
- Create: `heartbeat-web/src/components/admin/MobileModuleBrowser.jsx`
- Create: `heartbeat-web/src/components/admin/MobileResourceList.jsx`
- Create: `heartbeat-web/src/components/admin/MobileResourceDetail.jsx`
- Modify: `heartbeat-web/src/components/admin/ResourceDialog.jsx`
- Modify: `heartbeat-web/src/App.jsx`
- Modify: `heartbeat-web/src/styles.css`
- Test: `heartbeat-web/src/App.test.jsx`

- [ ] Add failing tests for the four bottom navigation destinations, module search, list-to-detail navigation, mobile create/edit/delete, and return navigation.
- [ ] Implement the mobile bottom navigation and searchable authorized-module browser.
- [ ] Implement summary lists and independent detail views using the same resource data and handlers as desktop.
- [ ] Use a floating create action and a fixed detail action bar for edit/delete.
- [ ] Turn the resource dialog into a full-screen mobile form below 768px while retaining the desktop modal.
- [ ] Add foldable/landscape dual-pane rules with a container/media query.
- [ ] Re-run frontend tests and confirm the mobile workflow passes.

### Task 5: Responsive verification and regression

**Files:**
- Modify: `heartbeat-web/src/styles.css`
- Modify: `heartbeat-web/src/App.test.jsx`
- Modify: `docs/superpowers/specs/2026-06-22-responsive-liquid-glass-admin-design.md`

- [ ] Add reduced-motion and unsupported-backdrop fallbacks.
- [ ] Verify focus states, touch target sizes, horizontal overflow, modal action visibility, and readable contrast in all three themes.
- [ ] Run `npm.cmd test -- --run` and `npm.cmd run build` in `heartbeat-web`.
- [ ] Run `mvn -pl heartbeat-start -am -Dtest=AdminPlatformApiTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- [ ] Review the implementation against every requirement in the design document and remove any stale showcase/glow styling that conflicts with the approved direction.

## Self-review

- Every design requirement maps to a task above.
- Theme names and API payloads consistently use `immersive`, `balanced`, and `professional`.
- Desktop and mobile share services and mutations but have separate presentation components.
- No placeholder task or deferred requirement remains.
