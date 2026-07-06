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
- Use a bounded workspace view cache for open tabs whose component-local state must survive tab switching. Inactive cached views stay mounted and hidden with CSS, while active view identity is driven by the URL.
- Do not rely on React Context alone for complex page state that lives inside component instances, uncontrolled inputs, focus state, scroll position, or third-party widgets.
- Because cached views do not unmount when hidden, page effects must be guarded by an active/inactive signal from the shell instead of assuming mount/unmount equals active/inactive.
- Cap or prune cached views to avoid memory growth. Generic resource pages can use lightweight state restoration; detail/form pages can use mounted view caching when they need true keep-alive behavior.
- Consider a lightweight state manager or route/component cache library only if the custom bounded cache becomes too complex or cannot preserve required state cleanly.

### Route Initialization Guard

Direct URL loads must not flash a false unknown-route state while IAM routes are still loading.

Initialization rules:

- Track IAM route loading explicitly with states such as `loading`, `ready`, and `fallback`.
- While route status is `loading`, render an admin-shell loading state and postpone unknown-route decisions.
- Only render unknown-route or unavailable-route states after authorized routes have loaded or the fallback route source has been selected.
- If route loading fails, use the existing fallback menu/module data and keep login/auth flows usable.

### History And Dynamic Tag Identity

The URL is the source of truth for the active workspace item. Tags are a remembered list of opened workspace entries; browser back/forward changes the active tag but does not implicitly close tags.

History rules:

- Browser back/forward should select or open the tag matching the new URL. It should not close other open tags.
- Closing an inactive tag only removes it from the tag list.
- Closing the active tag navigates to the nearest sensible remaining tag, preferring the left neighbor, then the right neighbor, then the default workspace tag.
- Closing the active tag should call `navigate(targetPath, { replace: true })` to avoid polluting browser history with a closed page.

Dynamic route rules:

- Use the normalized `location.pathname + location.search` as the tag instance key, not only the backend `menuId`.
- Keep the matched backend menu id as metadata for permission/resource mapping.
- Support backend route patterns such as `/admin/user/detail/:id` when matching direct URLs.
- Allow a page to report a dynamic tag title to the shell, for example changing `User Detail` to `User Detail - Alice`.

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
- Resource requests must be bound to the route/resource instance that started them.
- When the active tab, route instance, or resource changes, abort any in-flight resource request that is no longer relevant.
- Stale responses must not update the currently active module after the user has switched tabs.

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
- Apply automatic local surface downgrade for high-density views instead of relying only on users finding the flat-mode setting.
- If `backdrop-filter` is unsupported, `prefers-reduced-motion` is enabled, or a resource table exceeds the configured density threshold, force the affected content area to a more opaque balanced/restrained/flat surface while preserving the user's global visual preference elsewhere.
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
- Direct URL does not show unknown-route state before IAM route loading has completed or fallback routes have been selected.
- Browser back/forward changes the active tag without closing existing tags.
- Closing the active tag navigates with history replacement to the nearest remaining tag.
- Dynamic parameter URLs create distinct tag instances when their full path/search differs.
- Stale or aborted resource requests do not overwrite the active module after fast tab switching.
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
- Cached views invert the usual mount/unmount lifecycle. Effects in cached pages must respect active/inactive state to avoid background polling, duplicate fetches, or stale subscriptions.
- `backdrop-filter` can cause dropped frames on table-heavy screens. Glass performance hints and restrained/flat fallbacks are part of the implementation, not optional polish.
- Asynchronous IAM route loading can race with direct URL matching. Unknown-route decisions must wait for route initialization to settle.
- Fast tab switching can race with resource fetches. Requests need abort/stale-response guards.
- Dynamic hidden routes can open multiple instances. Tags must be keyed by URL instance, not only by backend menu id.
- Current source text includes encoding artifacts in some labels. This design does not require copy cleanup, but implementation should avoid making encoding churn worse.
- Glass effects can reduce readability if opacity is too low. Balanced/restrained defaults should keep admin workflows legible.

## Acceptance Criteria

- The admin shell uses real browser URLs for module navigation.
- Backend authorized routes drive top/side navigation.
- Open tags persist across reloads, normal tab switching preserves routine generic module view state, and cached pages receive active/inactive signals.
- Direct URL loads are guarded until IAM routes are ready or fallback routes are selected.
- Browser history, tag closing, and browser back/forward follow deterministic synchronization rules.
- Dynamic parameter routes can create multiple independent tag instances and update their displayed titles.
- Hidden menus are removed from navigation while remaining directly accessible if authorized.
- Disabled menus are removed from navigation and show unavailable state on direct access.
- Generic admin modules load their backend resources reliably.
- Fast tab switching does not allow stale resource responses to overwrite the active module.
- Menu settings changes are reflected after save without a full page reload.
- Major glass containers include performance-conscious CSS, dense pages automatically downgrade local glass intensity, and the UI remains readable and smooth enough under balanced/restrained modes.
- Frosted glass styling is polished, readable, and aligned with mainstream admin product conventions.
- Existing appearance preference behavior continues to work.
- Focused frontend tests pass.
