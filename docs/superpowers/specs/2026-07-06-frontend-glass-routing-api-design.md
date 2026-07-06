# Frontend Glass Routing And API Integration Design

Date: 2026-07-06
Status: Approved for implementation planning

## Context

The HeartBeat admin frontend lives in `heartbeat-web`, using React 18, Vite, `react-router-dom`, and a custom admin shell. The code already has:

- A frosted glass / liquid background visual system in `src/styles.css`, `src/theme/heartbeat-admin.css`, and `src/components/FluidBackground`.
- Appearance preferences persisted through `authApi.appearancePreference()` and `authApi.updateAppearancePreference()`.
- Backend API wrappers in `src/api.js` for auth, IAM routes/menus, admin resources, structure definitions, flow, monitor, tools, and payments.
- Dynamic menu and module helpers in `src/application/admin/adminModuleService.js`.
- Backend IAM endpoints for `/api/v1/iam/routes`, `/api/v1/iam/menus`, `/api/v1/iam/menus/tree-select`, and role menu assignment.

The current app is mostly state-routed inside `App.jsx`. Menu clicks switch `activeModuleKey`, but browser URLs do not drive the selected module. The header also exposes a visual-effects button that does not open the full settings controls on desktop.

## Product Direction

Use mainstream admin products as the design baseline: Ant Design Pro, Arco Design Pro, and RuoYi-style enterprise consoles. The target is a dense, predictable operational UI with a restrained frosted glass layer, not a decorative landing-page treatment.

The UI should feel:

- Professional and scannable for repeated admin work.
- Lightly premium through translucent surfaces, soft borders, and backdrop blur.
- Readable in tables, dialogs, forms, sidebars, and tags.
- Familiar to users of mainstream admin dashboards.

## Scope

Implement a frontend optimization pass that covers:

- Desktop and mobile admin shell navigation.
- Multi-tab/tag state preservation for routine admin page switching.
- Frosted glass surface treatment and appearance controls.
- URL-driven routing for backend menu routes.
- Backend API integration consistency for dynamic modules and menu settings.
- Menu visibility/status behavior.
- Focused tests for routing, menu visibility, API wiring, and appearance behavior.

Out of scope:

- Replacing the design system with Ant Design or another UI library.
- Changing backend authorization rules.
- Reworking generated persistence code.
- Adding unrelated business pages.
- Rebuilding the Flow Studio visual canvas.

## Recommended Approach

Use the approved Option C: mainstream admin layout with restrained frosted glass.

This keeps the current HeartBeat visual identity while tightening it around proven admin conventions:

- Sidebar + top module navigation.
- Tags/open tabs for visited modules.
- Dense tables and toolbars.
- Predictable dialogs and resource forms.
- Subtle glass only where it improves depth, never where it harms readability.

## Architecture

### Routing

Introduce real browser routing with `BrowserRouter`, `Routes`, route helpers, and navigation hooks.

The frontend should derive a stable app path for each menu item from backend data:

- Prefer `route.path` when present.
- Fall back to a deterministic admin path such as `/admin/module/:menuId`.
- Preserve existing special modules:
  - `home-dashboard`
  - `structure`
  - `flow`
  - `tool-gen`
  - `monitor-server`
  - `biz-pay-cashier`

Route synchronization rules:

- Clicking a sidebar/top/tag item navigates to the module URL and opens the matching tag.
- Loading a direct URL selects the matching module, top module, side menu, and tag.
- Switching among open tags should not discard routine in-page state such as table pagination, selected rows, filters, and unsaved form input where the current component model can reasonably preserve it.
- Unknown module URLs show a clear empty/error state inside the admin shell.
- Login/social auth redirects continue to land in the admin shell.

### Tab State Preservation

The open tags pattern should behave like mainstream admin multi-tab shells, where switching tabs does not casually destroy work in progress.

State preservation rules:

- Persist the open tag list and active tag in session storage so a reload can restore the user's workspace.
- Preserve lightweight per-module view state for generic resource pages, including selected row, table pagination, filters, and dialog draft values.
- Keep specialized pages responsible for their own state unless routing integration forces a shared shell concern.
- Prefer React Context plus local reducers for shell state because the codebase does not currently use a third-party state manager.
- Consider a lightweight state manager or route/component cache only if Context creates excessive prop threading or cannot preserve required state cleanly.
- Avoid mounting every visited page indefinitely by default. If CSS `display: none` or keep-alive caching is used, cap or prune cached views to avoid memory growth.

### Menu Data

Use `/api/v1/iam/routes` as the source of authorized navigation. If routes fail, retain the existing fallback route/module data so local and test flows still work.

Menu conventions:

- `type=DIR`: grouping node, shown as top or side group when `visible` is not `false` and `status` is not `DISABLED`.
- `type=MENU`: navigable module.
- `type=BUTTON`: permission/action only, never shown as a route item.
- `visible=false`: hide from sidebar/top navigation, but allow direct URL access if the user is authorized.
- `status=DISABLED`: treat as unavailable in the frontend. Do not show in navigation and show an unavailable state on direct access.

This matches mainstream admin behavior: "hidden" is often used for detail/edit pages that should not clutter navigation, while disabled means not currently usable.

### API Integration

Keep `src/api.js` as the single fetch wrapper layer, but make frontend consumption more consistent:

- Preserve session headers and Result envelope handling.
- Normalize route/menu payloads once in admin module helpers.
- Use `iamApi.menus()` for menu management tables.
- Use `iamApi.routes()` for navigation.
- Use `adminApi.resources(resource)` for generic admin resources.
- Keep specialized pages on their existing APIs:
  - `structureApi`
  - `flowApi`
  - `toolApi`
  - `monitorApi`
  - `authApi`

Resource loading rules:

- A route-backed generic module loads its bound resource when active and authenticated.
- Specialized pages own their own API loading.
- Refresh reloads the active module resource or specialized page data where supported.
- Create/update/delete refresh the active resource and, for menus, also refresh route navigation so settings changes are reflected.

### UI Composition

Keep the existing component boundaries, but reduce `App.jsx` responsibility enough that it can act as the route/layout coordinator rather than a catch-all application object.

- Route matching and URL generation helpers in `adminModuleService.js` or a small routing helper.
- Menu visibility/status normalization in the admin module service.
- An auth/session provider or hook for current user, auth check, login/logout, and social binding state.
- An appearance provider or hook wrapping the existing appearance preference API and local cache.
- A navigation/tags provider or hook for route tree, active module, top module, open tags, and tag persistence.
- A resource module hook for generic resource loading, mutation, refresh, selected rows, and per-module view state.

Avoid a broad rewrite. The implementation should improve the current shell in place.

## Visual Design

### Glass Treatment

Use restrained glass for shell surfaces:

- Header: translucent, sticky-looking command surface with strong text contrast.
- Sidebar: slightly more opaque than cards, stable dark/light readable treatment.
- Tags: compact pills with visible active state and close affordance.
- Cards/panels: translucent only in glass mode; solid enough for tables/forms.
- Dialogs: frosted modal over dimmed backdrop, with no nested card styling.

Glass tuning:

- Use blur/saturate with opaque fallback for unsupported browsers.
- Add GPU-composition hints such as `transform: translateZ(0)` to major frosted shell containers where it improves scroll smoothness.
- Use `will-change` sparingly and only on elements that animate or repeatedly change; do not blanket-apply it to tables, panels, or every glass surface.
- Prefer more opaque or restrained surfaces on DOM-heavy table pages to reduce backdrop-filter repaint cost.
- Keep border radius at 8px or less for admin cards/panels unless existing component style requires otherwise.
- Avoid giant decorative hero sections, marketing cards, and one-note color palettes.
- Keep table cells, labels, buttons, and sidebar items readable over fluid backgrounds.

### Appearance Settings

Make appearance controls feel intentional:

- The desktop header "theme and visual effects" trigger should open or focus the existing appearance settings panel rather than being inert.
- Keep color mode, accent color, visual style, and background motion controls.
- Keep user preference sync through existing appearance APIs.
- Surface mode (`immersive`, `balanced`, `restrained`, `flat`) remains a local admin-shell preference unless backend support already exists.

Default recommendation:

- Glass visual style enabled.
- Balanced surface mode for desktop admin work.
- Restrained mode available for denser screens.
- Flat mode available for low-power or high-contrast needs.

## Error Handling And States

Use consistent states across the shell:

- Loading state while auth/routes/resources are being fetched.
- Empty state for resources with no rows.
- Route unavailable state for disabled menus opened directly.
- Unknown route state for unmatched URLs.
- API error banner or inline message that does not break the shell.

The login page should still work if route loading fails after authentication.

## Testing

Frontend tests should cover:

- App mounts inside router and preserves login/auth behavior.
- Route data from `/api/v1/iam/routes` creates visible navigation.
- `visible=false` menu is absent from navigation but direct URL can render when authorized.
- `status=DISABLED` menu is absent and direct URL shows unavailable state.
- Menu click updates browser location and active module.
- Direct URL selects active module/top/sidebar/tag.
- Open tags and active tag restore from session storage.
- Switching tabs preserves generic resource view state such as selected rows, pagination/filter state, and in-progress dialog input where implemented.
- Menu CRUD refreshes both menu table data and route navigation.
- Appearance settings still persist through local cache and remote preference API.

Manual verification should include:

- `npm.cmd test`
- `npm.cmd run build`
- Desktop viewport navigation, table, dialog, and appearance settings checks.
- Mobile shell navigation and detail workflow checks.
- Scroll performance smoke check on a DOM-heavy table page with glass enabled and restrained/flat fallbacks.

## Risks

- `App.jsx` is large, so routing changes must be incremental and well-tested.
- Backend route paths may not always map cleanly to current frontend module keys. The route helper must support both backend `path` and fallback module URLs.
- Keeping too many tab views mounted can increase memory use. The implementation should preserve important shell state without unbounded keep-alive caching.
- `backdrop-filter` can cause dropped frames on table-heavy screens. Glass performance hints and restrained/flat fallbacks are part of the implementation, not optional polish.
- Current source text includes encoding artifacts in some labels. This design does not require copy cleanup, but implementation should avoid making encoding churn worse.
- Glass effects can reduce readability if opacity is too low. Balanced/restrained defaults should keep admin workflows legible.

## Acceptance Criteria

- The admin shell uses real browser URLs for module navigation.
- Backend authorized routes drive top/side navigation.
- Open tags persist across reloads, and normal tab switching preserves routine generic module view state.
- Hidden menus are removed from navigation while remaining directly accessible if authorized.
- Disabled menus are removed from navigation and show unavailable state on direct access.
- Generic admin modules load their backend resources reliably.
- Menu settings changes are reflected after save without a full page reload.
- Major glass containers include performance-conscious CSS, and dense pages remain readable and smooth enough under balanced/restrained modes.
- Frosted glass styling is polished, readable, and aligned with mainstream admin product conventions.
- Existing appearance preference behavior continues to work.
- Focused frontend tests pass.
