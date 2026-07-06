# Frontend Glass Routing API DDD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved HeartBeat admin frontend shell with backend-driven routes, restrained frosted glass, multi-tab state persistence, and DDD-style frontend boundaries.

**Architecture:** Keep infrastructure concerns in API/storage adapters, encode navigation/menu/workspace rules as pure domain functions, and expose app workflows through focused hooks. `App.jsx` becomes the composition shell instead of owning auth, routing, tags, resources, and visual settings directly.

**Tech Stack:** React 18, Vite, React Router DOM 6, Vitest, Testing Library, browser `sessionStorage`, existing HeartBeat CSS/theme system.

---

## File Structure

- Create `heartbeat-web/src/domain/admin/navigationPolicy.js`: pure menu/route policy functions: normalize route paths, filter visible navigation, detect disabled nodes, find route by URL/module id, derive top module, derive default menu.
- Create `heartbeat-web/src/domain/admin/navigationPolicy.test.js`: tests for hidden menus, disabled menus, path fallback, special routes, and top-module resolution.
- Create `heartbeat-web/src/domain/admin/workspaceState.js`: pure workspace/tag state functions: normalize tags, open/select/close tags, serialize/restore state, update per-module view state.
- Create `heartbeat-web/src/domain/admin/workspaceState.test.js`: tests for tag persistence rules and per-module view state.
- Create `heartbeat-web/src/infrastructure/browser/workspaceStorage.js`: small sessionStorage adapter with safe JSON parsing.
- Create `heartbeat-web/src/infrastructure/browser/workspaceStorage.test.js`: storage adapter tests.
- Create `heartbeat-web/src/application/admin/useAdminSession.js`: auth/session/social-login workflow hook extracted from `App.jsx`.
- Create `heartbeat-web/src/application/admin/useAdminNavigation.js`: route tree loading, active module path synchronization, tags persistence, and route refresh workflow.
- Create `heartbeat-web/src/application/admin/useAdminResources.js`: generic resource list/mutation workflow, selected-row/view-state persistence, and menu CRUD route refresh callback.
- Modify `heartbeat-web/src/application/admin/adminModuleService.js`: keep resource mapping and module building, but delegate menu visibility/path policy to domain helpers where useful.
- Modify `heartbeat-web/src/main.jsx`: wrap `App` in `BrowserRouter`.
- Modify `heartbeat-web/src/App.jsx`: consume new hooks, use URL-driven navigation, keep specialized pages in place, and render unavailable/unknown route states.
- Modify `heartbeat-web/src/components/admin/ResourceDialog.jsx`: optionally accept `onDraftChange` and persist draft values while a tag is open.
- Modify `heartbeat-web/src/components/admin/ResourceTable.jsx`: add minimal pagination props/state support if needed for view-state tests.
- Modify `heartbeat-web/src/layout/LayoutHeader.jsx`: make the visual-effects button focus/toggle appearance settings instead of doing nothing.
- Modify `heartbeat-web/src/components/AppearanceSettingsPanel/AppearanceSettingsPanel.jsx`: support controlled open/focus from shell.
- Modify `heartbeat-web/src/theme/heartbeat-admin.css` and `heartbeat-web/src/styles.css`: restrained glass polish, GPU composition hints, restrained dense-page fallback.
- Modify `heartbeat-web/src/App.test.jsx`: integration tests for route-driven navigation, hidden/disabled menus, tag persistence, menu CRUD route refresh, and appearance behavior.

---

## Task 1: Navigation Domain Policy

**Files:**
- Create: `heartbeat-web/src/domain/admin/navigationPolicy.js`
- Create: `heartbeat-web/src/domain/admin/navigationPolicy.test.js`
- Modify: `heartbeat-web/src/application/admin/adminModuleService.js`

- [ ] **Step 1: Write failing domain tests**

Add `heartbeat-web/src/domain/admin/navigationPolicy.test.js`:

```js
import {describe, expect, test} from 'vitest'
import {
  appPathForMenu,
  filterNavigableTree,
  findMenuByAppPath,
  firstAvailableMenu,
  isDisabledMenu,
  isVisibleMenu,
  resolveTopModuleIdByPath
} from './navigationPolicy'

const routeTree = [
  {
    id: 'system',
    name: 'System',
    type: 'DIR',
    path: '/system',
    children: [
      {id: 'system-user', name: 'Users', type: 'MENU', path: '/system/user', permission: 'system:user:list'},
      {id: 'system-secret', name: 'Secret', type: 'MENU', path: '/system/secret', visible: false},
      {id: 'system-disabled', name: 'Disabled', type: 'MENU', path: '/system/disabled', status: 'DISABLED'},
      {id: 'system-user-add', name: 'Add', type: 'BUTTON', permission: 'system:user:add'}
    ]
  }
]

describe('navigation policy', () => {
  test('hides invisible and disabled menus from navigation but keeps hidden routes matchable', () => {
    const navigable = filterNavigableTree(routeTree)

    expect(navigable[0].children.map((item) => item.id)).toEqual(['system-user'])
    expect(findMenuByAppPath(routeTree, '/system/secret')?.id).toBe('system-secret')
    expect(findMenuByAppPath(routeTree, '/system/disabled')?.id).toBe('system-disabled')
  })

  test('detects visibility and disabled status explicitly', () => {
    expect(isVisibleMenu({visible: false})).toBe(false)
    expect(isVisibleMenu({visible: true})).toBe(true)
    expect(isVisibleMenu({})).toBe(true)
    expect(isDisabledMenu({status: 'DISABLED'})).toBe(true)
    expect(isDisabledMenu({status: 'ACTIVE'})).toBe(false)
  })

  test('derives app paths from backend path or deterministic fallback', () => {
    expect(appPathForMenu({id: 'system-user', path: '/system/user'})).toBe('/system/user')
    expect(appPathForMenu({id: 'generated-report'})).toBe('/admin/module/generated-report')
    expect(appPathForMenu({id: 'structure'})).toBe('/structure')
  })

  test('resolves first available menu and top module from direct URL', () => {
    expect(firstAvailableMenu(routeTree)?.id).toBe('system-user')
    expect(resolveTopModuleIdByPath(routeTree, '/system/user')).toBe('system')
    expect(resolveTopModuleIdByPath(routeTree, '/system/secret')).toBe('system')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
npm.cmd test -- navigationPolicy.test.js --run
```

Expected: FAIL because `navigationPolicy.js` does not exist.

- [ ] **Step 3: Implement navigation policy**

Add `heartbeat-web/src/domain/admin/navigationPolicy.js`:

```js
const SPECIAL_PATHS = {
  'home-dashboard': '/dashboard',
  structure: '/structure',
  flow: '/flow',
  'tool-gen': '/tool/gen',
  'monitor-server': '/monitor/server',
  'biz-pay-cashier': '/pay/cashier'
}

export function isVisibleMenu(menu) {
  return menu?.visible !== false
}

export function isDisabledMenu(menu) {
  return String(menu?.status || '').toUpperCase() === 'DISABLED'
}

export function isNavigableMenu(menu) {
  return menu?.type === 'MENU' && isVisibleMenu(menu) && !isDisabledMenu(menu)
}

export function normalizeAppPath(path) {
  if (!path || path === '#') return ''
  const withSlash = String(path).startsWith('/') ? String(path) : `/${path}`
  return withSlash.replace(/\/+/g, '/').replace(/\/$/, '') || '/'
}

export function appPathForMenu(menu) {
  if (!menu?.id) return '/admin'
  if (SPECIAL_PATHS[menu.id]) return SPECIAL_PATHS[menu.id]
  const normalized = normalizeAppPath(menu.path)
  return normalized || `/admin/module/${encodeURIComponent(menu.id)}`
}

function cloneWithChildren(menu, children) {
  return {...menu, path: appPathForMenu(menu), children}
}

export function filterNavigableTree(nodes = []) {
  return nodes
      .filter((node) => node.type !== 'BUTTON' && isVisibleMenu(node) && !isDisabledMenu(node))
      .map((node) => {
        const children = filterNavigableTree(node.children || [])
        if (node.type === 'DIR') return cloneWithChildren(node, children)
        return cloneWithChildren(node, children)
      })
      .filter((node) => node.type === 'MENU' || (node.children || []).length > 0)
}

export function flattenMenus(nodes = [], target = []) {
  nodes.forEach((node) => {
    if (node.type === 'MENU') target.push({...node, path: appPathForMenu(node)})
    flattenMenus(node.children || [], target)
  })
  return target
}

export function firstAvailableMenu(nodes = []) {
  for (const node of nodes) {
    if (isNavigableMenu(node)) return {...node, path: appPathForMenu(node)}
    const child = firstAvailableMenu(node.children || [])
    if (child) return child
  }
  return null
}

export function findMenuById(nodes = [], id) {
  for (const node of nodes) {
    if (node.id === id) return {...node, path: appPathForMenu(node)}
    const child = findMenuById(node.children || [], id)
    if (child) return child
  }
  return null
}

export function findMenuByAppPath(nodes = [], path) {
  const normalized = normalizeAppPath(path)
  return flattenMenus(nodes).find((menu) => appPathForMenu(menu) === normalized) || null
}

export function resolveTopModuleIdByPath(nodes = [], path) {
  const normalized = normalizeAppPath(path)
  for (const top of nodes) {
    if (appPathForMenu(top) === normalized) return top.id
    if (findMenuByAppPath(top.children || [], normalized)) return top.id
  }
  return nodes[0]?.id || ''
}
```

Modify `heartbeat-web/src/application/admin/adminModuleService.js` imports and helpers:

```js
import {
    appPathForMenu,
    filterNavigableTree,
    findMenuById,
    firstAvailableMenu,
    flattenMenus,
    resolveTopModuleIdByPath
} from '../../domain/admin/navigationPolicy'
```

Then replace local flatten/first/resolve helper internals to call the domain helpers while preserving exported names:

```js
export function flattenRouteModules(routes = [], target = []) {
  flattenMenus(routes).forEach((route) => target.push(moduleFromRoute(route)))
  return target
}

export function splitTopSideMenus(routeTree = []) {
  return filterNavigableTree(routeTree).filter((route) => route.type === 'DIR')
}

export function sideMenusForTop(routeTree = [], topModuleId) {
  const top = filterNavigableTree(routeTree).find((item) => item.id === topModuleId)
  return top?.children || []
}

export function resolveTopModuleId(routeTree = [], menuIdOrPath) {
  if (String(menuIdOrPath || '').startsWith('/')) {
    return resolveTopModuleIdByPath(routeTree, menuIdOrPath)
  }
  const menu = findMenuById(routeTree, menuIdOrPath)
  return menu ? resolveTopModuleIdByPath(routeTree, appPathForMenu(menu)) : routeTree[0]?.id
}

export function firstMenuInTree(nodes = []) {
  return firstAvailableMenu(nodes)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
npm.cmd test -- navigationPolicy.test.js --run
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add -- heartbeat-web/src/domain/admin/navigationPolicy.js heartbeat-web/src/domain/admin/navigationPolicy.test.js heartbeat-web/src/application/admin/adminModuleService.js
git commit -m "feat: add admin navigation domain policy"
```

---

## Task 2: Workspace Tags Domain And Storage

**Files:**
- Create: `heartbeat-web/src/domain/admin/workspaceState.js`
- Create: `heartbeat-web/src/domain/admin/workspaceState.test.js`
- Create: `heartbeat-web/src/infrastructure/browser/workspaceStorage.js`
- Create: `heartbeat-web/src/infrastructure/browser/workspaceStorage.test.js`

- [ ] **Step 1: Write failing workspace domain tests**

Add `heartbeat-web/src/domain/admin/workspaceState.test.js`:

```js
import {describe, expect, test} from 'vitest'
import {
  closeWorkspaceTag,
  DEFAULT_WORKSPACE_TAG,
  openWorkspaceTag,
  restoreWorkspaceState,
  updateModuleViewState
} from './workspaceState'

describe('workspace state', () => {
  test('opens a tag once and selects it', () => {
    const state = restoreWorkspaceState()
    const next = openWorkspaceTag(state, {id: 'system-user', name: 'Users', path: '/system/user'})

    expect(next.activeTagId).toBe('system-user')
    expect(next.tags.map((tag) => tag.id)).toEqual([DEFAULT_WORKSPACE_TAG.id, 'system-user'])
    expect(openWorkspaceTag(next, {id: 'system-user', name: 'Users', path: '/system/user'}).tags).toHaveLength(2)
  })

  test('does not close non-closable default tag and falls back to previous tag', () => {
    const state = openWorkspaceTag(restoreWorkspaceState(), {id: 'system-user', name: 'Users', path: '/system/user'})

    expect(closeWorkspaceTag(state, DEFAULT_WORKSPACE_TAG.id).tags[0].id).toBe(DEFAULT_WORKSPACE_TAG.id)
    expect(closeWorkspaceTag(state, 'system-user').activeTagId).toBe(DEFAULT_WORKSPACE_TAG.id)
  })

  test('restores valid serialized tags and preserves per-module view state', () => {
    const restored = restoreWorkspaceState({
      activeTagId: 'system-user',
      tags: [{id: 'system-user', name: 'Users', path: '/system/user'}],
      moduleViewState: { 'system-user': { selectedRowId: '2' } }
    })
    const updated = updateModuleViewState(restored, 'system-user', { dialogDraft: { name: 'Alice' } })

    expect(updated.activeTagId).toBe('system-user')
    expect(updated.moduleViewState['system-user']).toEqual({
      selectedRowId: '2',
      dialogDraft: { name: 'Alice' }
    })
  })
})
```

Add `heartbeat-web/src/infrastructure/browser/workspaceStorage.test.js`:

```js
import {afterEach, describe, expect, test} from 'vitest'
import {readWorkspaceState, writeWorkspaceState} from './workspaceStorage'

afterEach(() => window.sessionStorage.clear())

describe('workspace storage', () => {
  test('round-trips workspace state through session storage', () => {
    const state = {activeTagId: 'system-user', tags: [{id: 'system-user', name: 'Users'}]}

    writeWorkspaceState(state)

    expect(readWorkspaceState()).toEqual(state)
  })

  test('returns null for corrupt workspace state', () => {
    window.sessionStorage.setItem('heartbeat_admin_workspace', '{')

    expect(readWorkspaceState()).toBeNull()
  })
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
npm.cmd test -- workspaceState.test.js workspaceStorage.test.js --run
```

Expected: FAIL because the new modules do not exist.

- [ ] **Step 3: Implement workspace domain**

Add `heartbeat-web/src/domain/admin/workspaceState.js`:

```js
export const DEFAULT_WORKSPACE_TAG = Object.freeze({
  id: 'structure',
  name: '结构展示配置',
  path: '/structure',
  closable: false
})

function normalizeTag(tag) {
  if (!tag?.id) return null
  return {
    id: String(tag.id),
    name: tag.name || String(tag.id),
    path: tag.path || `/admin/module/${encodeURIComponent(tag.id)}`,
    closable: tag.closable === false ? false : true
  }
}

export function restoreWorkspaceState(saved) {
  const tags = Array.isArray(saved?.tags)
      ? saved.tags.map(normalizeTag).filter(Boolean)
      : []
  const hasDefault = tags.some((tag) => tag.id === DEFAULT_WORKSPACE_TAG.id)
  const normalizedTags = hasDefault ? tags : [DEFAULT_WORKSPACE_TAG, ...tags]
  const activeTagId = normalizedTags.some((tag) => tag.id === saved?.activeTagId)
      ? saved.activeTagId
      : normalizedTags[0].id
  return {
    activeTagId,
    tags: normalizedTags,
    moduleViewState: saved?.moduleViewState && typeof saved.moduleViewState === 'object'
        ? saved.moduleViewState
        : {}
  }
}

export function openWorkspaceTag(state, tag) {
  const normalized = normalizeTag(tag)
  if (!normalized) return restoreWorkspaceState(state)
  const current = restoreWorkspaceState(state)
  const tags = current.tags.some((item) => item.id === normalized.id)
      ? current.tags
      : [...current.tags, normalized]
  return {...current, tags, activeTagId: normalized.id}
}

export function selectWorkspaceTag(state, tagId) {
  const current = restoreWorkspaceState(state)
  return current.tags.some((tag) => tag.id === tagId)
      ? {...current, activeTagId: tagId}
      : current
}

export function closeWorkspaceTag(state, tagId) {
  const current = restoreWorkspaceState(state)
  const closing = current.tags.find((tag) => tag.id === tagId)
  if (!closing || closing.closable === false) return current
  const tags = current.tags.filter((tag) => tag.id !== tagId)
  const activeTagId = current.activeTagId === tagId
      ? tags[tags.length - 1]?.id || DEFAULT_WORKSPACE_TAG.id
      : current.activeTagId
  return {...current, tags, activeTagId}
}

export function updateModuleViewState(state, moduleId, patch) {
  const current = restoreWorkspaceState(state)
  return {
    ...current,
    moduleViewState: {
      ...current.moduleViewState,
      [moduleId]: {
        ...(current.moduleViewState[moduleId] || {}),
        ...patch
      }
    }
  }
}
```

- [ ] **Step 4: Implement storage adapter**

Add `heartbeat-web/src/infrastructure/browser/workspaceStorage.js`:

```js
const WORKSPACE_KEY = 'heartbeat_admin_workspace'

export function readWorkspaceState(storage = window.sessionStorage) {
  try {
    const raw = storage.getItem(WORKSPACE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function writeWorkspaceState(state, storage = window.sessionStorage) {
  try {
    storage.setItem(WORKSPACE_KEY, JSON.stringify(state))
  } catch {
    // Storage quota or privacy mode should not break navigation.
  }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run:

```powershell
npm.cmd test -- workspaceState.test.js workspaceStorage.test.js --run
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add -- heartbeat-web/src/domain/admin/workspaceState.js heartbeat-web/src/domain/admin/workspaceState.test.js heartbeat-web/src/infrastructure/browser/workspaceStorage.js heartbeat-web/src/infrastructure/browser/workspaceStorage.test.js
git commit -m "feat: add admin workspace state model"
```

---

## Task 3: Application Hooks For Session, Navigation, And Resources

**Files:**
- Create: `heartbeat-web/src/application/admin/useAdminSession.js`
- Create: `heartbeat-web/src/application/admin/useAdminNavigation.js`
- Create: `heartbeat-web/src/application/admin/useAdminResources.js`
- Test: `heartbeat-web/src/App.test.jsx`

- [ ] **Step 1: Add failing integration tests for route and tag behavior**

Append to `heartbeat-web/src/App.test.jsx`:

```jsx
test('opens backend route by URL and persists the active tag', async () => {
  mockLoggedIn()
  window.history.pushState({}, '', '/system/user')
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
    if (url === '/api/v1/auth/me') return authMeResponse()
    if (url === '/api/v1/iam/routes') {
      return {
        ok: true,
        json: async () => ({
          code: '0',
          msg: 'success',
          data: [{
            id: 'system',
            name: '系统管理',
            type: 'DIR',
            children: [{id: 'system-user', name: '用户管理', type: 'MENU', path: '/system/user', permission: 'system:user:list'}]
          }]
        })
      }
    }
    if (url === '/api/v1/admin/resources/users') {
      return {
        ok: true,
        json: async () => ({code: '0', msg: 'success', data: [{id: '2', username: 'alice', nickname: 'Alice'}]})
      }
    }
    return adminModulesResponse()
  })

  render(<App />)

  expect(await screen.findByRole('heading', {name: '用户管理', level: 1})).toBeInTheDocument()
  expect(JSON.parse(window.sessionStorage.getItem('heartbeat_admin_workspace')).activeTagId).toBe('system-user')
})

test('hides invisible routes from navigation but allows direct access', async () => {
  mockLoggedIn()
  window.history.pushState({}, '', '/system/secret')
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
    if (url === '/api/v1/auth/me') return authMeResponse()
    if (url === '/api/v1/iam/routes') {
      return {
        ok: true,
        json: async () => ({
          code: '0',
          msg: 'success',
          data: [{
            id: 'system',
            name: '系统管理',
            type: 'DIR',
            children: [
              {id: 'system-user', name: '用户管理', type: 'MENU', path: '/system/user', permission: 'system:user:list'},
              {id: 'system-secret', name: '隐藏菜单', type: 'MENU', path: '/system/secret', visible: false, permission: 'system:secret:list'}
            ]
          }]
        })
      }
    }
    return adminModulesResponse()
  })

  render(<App />)

  const nav = await screen.findByLabelText('后台导航')
  expect(within(nav).queryByText('隐藏菜单')).not.toBeInTheDocument()
  expect(await screen.findByRole('heading', {name: '隐藏菜单', level: 1})).toBeInTheDocument()
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
npm.cmd test -- App.test.jsx --run
```

Expected: FAIL because `App` is not router-driven and does not persist workspace state.

- [ ] **Step 3: Extract session hook**

Create `heartbeat-web/src/application/admin/useAdminSession.js`:

```js
import {useEffect, useState} from 'react'
import {authApi} from '../../api'

function rememberSessionUser(user) {
  if (!user?.id) return
  try {
    const session = JSON.parse(localStorage.getItem('heartbeat_admin_session') || '{}')
    localStorage.setItem('heartbeat_admin_session', JSON.stringify({...session, userId: user.id}))
  } catch {
    localStorage.setItem('heartbeat_admin_session', JSON.stringify({userId: user.id}))
  }
}

export function saveAuthSession(result) {
  localStorage.setItem('heartbeat_admin_session', JSON.stringify({
    accessToken: result.accessToken,
    refreshToken: result.refreshToken,
    userId: result.user?.id
  }))
}

export default function useAdminSession(run) {
  const [currentUser, setCurrentUser] = useState(null)
  const [authChecked, setAuthChecked] = useState(false)
  const [socialProviders, setSocialProviders] = useState([])
  const [pendingBind, setPendingBind] = useState(null)

  useEffect(() => {
    let mounted = true
    const saved = localStorage.getItem('heartbeat_admin_session')
    if (!saved) {
      setAuthChecked(true)
      return () => { mounted = false }
    }
    authApi.me()
        .then((user) => {
          if (mounted) {
            rememberSessionUser(user)
            setCurrentUser(user)
          }
        })
        .catch(() => localStorage.removeItem('heartbeat_admin_session'))
        .finally(() => {
          if (mounted) setAuthChecked(true)
        })
    return () => { mounted = false }
  }, [])

  useEffect(() => {
    if (!authChecked || currentUser) return undefined
    let mounted = true
    authApi.socialProviders()
        .then((items) => {
          if (mounted) setSocialProviders(Array.isArray(items) ? items : [])
        })
        .catch(() => {
          if (mounted) setSocialProviders([])
        })
    return () => { mounted = false }
  }, [authChecked, currentUser])

  async function logout() {
    await run('logout', async () => {
      await authApi.logout()
      localStorage.removeItem('heartbeat_admin_session')
      setCurrentUser(null)
    })
  }

  return {
    currentUser,
    setCurrentUser,
    authChecked,
    socialProviders,
    pendingBind,
    setPendingBind,
    logout
  }
}
```

- [ ] **Step 4: Extract navigation hook**

Create `heartbeat-web/src/application/admin/useAdminNavigation.js`:

```js
import {useCallback, useEffect, useMemo, useState} from 'react'
import {useLocation, useNavigate} from 'react-router-dom'
import {adminApi, iamApi} from '../../api'
import {
  appPathForMenu,
  filterNavigableTree,
  findMenuByAppPath,
  findMenuById,
  firstAvailableMenu,
  isDisabledMenu,
  resolveTopModuleIdByPath
} from '../../domain/admin/navigationPolicy'
import {
  closeWorkspaceTag,
  openWorkspaceTag,
  restoreWorkspaceState,
  selectWorkspaceTag,
  updateModuleViewState
} from '../../domain/admin/workspaceState'
import {readWorkspaceState, writeWorkspaceState} from '../../infrastructure/browser/workspaceStorage'
import {flattenRouteModules} from './adminModuleService'

export default function useAdminNavigation({fallbackAdminModules, fallbackRouteTree, initialModuleKey = 'structure'}) {
  const navigate = useNavigate()
  const location = useLocation()
  const [routeTree, setRouteTree] = useState([])
  const [adminModules, setAdminModules] = useState(fallbackAdminModules)
  const [workspace, setWorkspace] = useState(() => restoreWorkspaceState(readWorkspaceState() || {
    activeTagId: initialModuleKey
  }))

  const navigationTree = useMemo(
      () => filterNavigableTree(routeTree.length > 0 ? routeTree : fallbackRouteTree),
      [routeTree, fallbackRouteTree]
  )
  const activeModuleKey = workspace.activeTagId
  const activeMenu = useMemo(
      () => findMenuById(routeTree, activeModuleKey) || findMenuById(fallbackRouteTree, activeModuleKey),
      [routeTree, fallbackRouteTree, activeModuleKey]
  )
  const activeTopModuleKey = useMemo(
      () => resolveTopModuleIdByPath(routeTree.length > 0 ? routeTree : fallbackRouteTree, location.pathname),
      [routeTree, fallbackRouteTree, location.pathname]
  )
  const activeModuleViewState = workspace.moduleViewState[activeModuleKey] || {}

  const persistWorkspace = useCallback((next) => {
    const restored = restoreWorkspaceState(next)
    setWorkspace(restored)
    writeWorkspaceState(restored)
    return restored
  }, [])

  const refreshRoutes = useCallback(async () => {
    try {
      const routes = await iamApi.routes()
      if (Array.isArray(routes)) {
        setRouteTree(routes)
        const modules = flattenRouteModules(routes)
        setAdminModules(modules.length > 0 ? modules : fallbackAdminModules)
      }
    } catch {
      const modules = await adminApi.modules()
      if (Array.isArray(modules)) setAdminModules(modules.length > 0 ? modules : fallbackAdminModules)
    }
  }, [fallbackAdminModules])

  useEffect(() => {
    refreshRoutes()
  }, [refreshRoutes])

  useEffect(() => {
    const tree = routeTree.length > 0 ? routeTree : fallbackRouteTree
    const menu = findMenuByAppPath(tree, location.pathname)
    if (menu) {
      persistWorkspace(openWorkspaceTag(workspace, {
        id: menu.id,
        name: menu.name,
        path: appPathForMenu(menu),
        closable: menu.closable
      }))
      return
    }
    if (location.pathname === '/' || location.pathname === '/admin') {
      const first = firstAvailableMenu(tree)
      if (first) navigate(appPathForMenu(first), {replace: true})
    }
  }, [location.pathname, routeTree])

  function openMenu(menu) {
    if (!menu?.id) return
    const path = appPathForMenu(menu)
    persistWorkspace(openWorkspaceTag(workspace, {id: menu.id, name: menu.name, path}))
    navigate(path)
  }

  function selectTag(tagId) {
    const next = persistWorkspace(selectWorkspaceTag(workspace, tagId))
    const tag = next.tags.find((item) => item.id === tagId)
    if (tag?.path) navigate(tag.path)
  }

  function closeTag(tagId) {
    const next = persistWorkspace(closeWorkspaceTag(workspace, tagId))
    const tag = next.tags.find((item) => item.id === next.activeTagId)
    if (tag?.path) navigate(tag.path)
  }

  function patchActiveModuleViewState(patch) {
    persistWorkspace(updateModuleViewState(workspace, activeModuleKey, patch))
  }

  return {
    routeTree,
    navigationTree,
    adminModules,
    activeModuleKey,
    activeMenu,
    activeTopModuleKey,
    tags: workspace.tags,
    activeModuleViewState,
    unavailableRoute: activeMenu && isDisabledMenu(activeMenu),
    refreshRoutes,
    openMenu,
    selectTag,
    closeTag,
    patchActiveModuleViewState
  }
}
```

- [ ] **Step 5: Extract resource hook**

Create `heartbeat-web/src/application/admin/useAdminResources.js`:

```js
import {useCallback, useEffect, useState} from 'react'
import {adminApi, iamApi, toolApi} from '../../api'
import {
  buildResourcePayload,
  flattenMenuRows,
  isResourceReadOnly,
  recordFromResource
} from './adminModuleService'

export default function useAdminResources({
  currentUser,
  activeModuleKey,
  activeResource,
  activeColumns,
  activeAdminModule,
  run,
  onError,
  onRoutesChanged,
  moduleViewState,
  patchModuleViewState
}) {
  const [moduleData, setModuleData] = useState({})
  const [resourceDialog, setResourceDialog] = useState({open: false, mode: 'create', row: null})
  const selectedResourceRow = moduleViewState.selectedRow || null

  const loadModuleResource = useCallback(async (resource = activeResource) => {
    if (!resource) return []
    const items = resource === 'menus' ? await iamApi.menus() : await adminApi.resources(resource)
    const rows = resource === 'menus'
        ? flattenMenuRows(items)
        : items.map((item) => recordFromResource(resource, item))
    setModuleData((previous) => ({...previous, [resource]: rows}))
    return rows
  }, [activeResource])

  useEffect(() => {
    if (!currentUser || activeModuleKey === 'structure' || !activeResource) return
    loadModuleResource(activeResource)
    patchModuleViewState({selectedRow: null})
  }, [currentUser, activeModuleKey, activeResource, loadModuleResource])

  function selectResourceRow(row) {
    patchModuleViewState({selectedRow: row})
  }

  function openResourceDialog(mode, row = null) {
    onError('')
    setResourceDialog({open: true, mode, row})
    patchModuleViewState({dialogDraft: null})
  }

  function closeResourceDialog() {
    setResourceDialog({open: false, mode: 'create', row: null})
    patchModuleViewState({dialogDraft: null})
  }

  async function submitResourceDialog(values) {
    await run(`resource-${resourceDialog.mode}`, async () => {
      const payload = buildResourcePayload(activeResource, values, resourceDialog.mode)
      if (resourceDialog.mode === 'edit') {
        const id = resourceDialog.row?.id
        if (activeResource === 'menus') await iamApi.updateMenu(id, payload)
        else await adminApi.updateResource(activeResource, id, payload)
      } else if (activeResource === 'menus') {
        await iamApi.createMenu(payload)
      } else {
        await adminApi.createResource(activeResource, payload)
      }
      closeResourceDialog()
      await loadModuleResource(activeResource)
      if (activeResource === 'menus') await onRoutesChanged?.()
    })
  }

  async function deleteResourceRow(row) {
    if (typeof window.confirm === 'function' && !window.confirm(`确认删除 ${row[activeColumns[0]] || row.id} ?`)) return
    await run('resource-delete', async () => {
      if (activeResource === 'menus') await iamApi.deleteMenu(row.id)
      else await adminApi.deleteResource(activeResource, row.id)
      patchModuleViewState({selectedRow: null})
      await loadModuleResource(activeResource)
      if (activeResource === 'menus') await onRoutesChanged?.()
    })
  }

  async function handleJobAction(action) {
    if (!selectedResourceRow?.id) {
      onError('请选择任务')
      return
    }
    await run(`job-${action}`, async () => {
      if (action === 'run') await toolApi.runJob(selectedResourceRow.id)
      if (action === 'pause') await toolApi.pauseJob(selectedResourceRow.id)
      if (action === 'resume') await toolApi.resumeJob(selectedResourceRow.id)
      if (action === 'refresh') await toolApi.refreshJobs()
      await loadModuleResource('jobs')
    })
  }

  return {
    moduleData,
    activeRecords: activeResource ? (moduleData[activeResource] || []) : [],
    resourceDialog,
    selectedResourceRow,
    resourceEditable: Boolean(activeResource) && !isResourceReadOnly(activeResource),
    selectResourceRow,
    openResourceDialog,
    closeResourceDialog,
    submitResourceDialog,
    deleteResourceRow,
    loadModuleResource,
    handleJobAction,
    onDialogDraftChange: (dialogDraft) => patchModuleViewState({dialogDraft})
  }
}
```

- [ ] **Step 6: Run tests and commit hooks**

Run:

```powershell
npm.cmd test -- App.test.jsx navigationPolicy.test.js workspaceState.test.js workspaceStorage.test.js --run
```

Expected: existing app tests may still fail until Task 4 integrates the hooks; new hook files should compile.

Commit only if the suite failure is the expected App integration failure:

```powershell
git add -- heartbeat-web/src/application/admin/useAdminSession.js heartbeat-web/src/application/admin/useAdminNavigation.js heartbeat-web/src/application/admin/useAdminResources.js heartbeat-web/src/App.test.jsx
git commit -m "test: capture route driven admin shell behavior"
```

---

## Task 4: Router-Driven App Shell Integration

**Files:**
- Modify: `heartbeat-web/src/main.jsx`
- Modify: `heartbeat-web/src/App.jsx`
- Modify: `heartbeat-web/src/layout/AdminLayout.jsx`
- Modify: `heartbeat-web/src/layout/LayoutHeader.jsx`
- Modify: `heartbeat-web/src/layout/LayoutSider.jsx`
- Modify: `heartbeat-web/src/layout/TagsView.jsx`

- [ ] **Step 1: Wrap app in BrowserRouter**

Modify `heartbeat-web/src/main.jsx`:

```jsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import {BrowserRouter} from 'react-router-dom'
import App from './App'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
)
```

- [ ] **Step 2: Replace App-owned auth/navigation/resource state with hooks**

In `heartbeat-web/src/App.jsx`, import the hooks:

```js
import useAdminNavigation from './application/admin/useAdminNavigation'
import useAdminResources from './application/admin/useAdminResources'
import useAdminSession, {saveAuthSession} from './application/admin/useAdminSession'
```

Remove the local `currentUser`, `socialProviders`, `pendingBind`, `authChecked`, `routeTree`, `activeTopModuleKey`, `openTags`, `activeModuleKey`, `moduleData`, `resourceDialog`, and `selectedResourceRow` state declarations. Replace them with:

```js
const session = useAdminSession(run)
const {
  currentUser,
  setCurrentUser,
  authChecked,
  socialProviders,
  pendingBind,
  setPendingBind,
  logout
} = session

const navigation = useAdminNavigation({
  fallbackAdminModules,
  fallbackRouteTree: fallbackSideMenus,
  initialModuleKey: 'structure'
})

const {
  adminModules,
  navigationTree,
  activeModuleKey,
  activeMenu,
  activeTopModuleKey,
  tags,
  activeModuleViewState,
  unavailableRoute,
  refreshRoutes,
  openMenu,
  selectTag,
  closeTag,
  patchActiveModuleViewState
} = navigation
```

Then wire resources:

```js
const resourceState = useAdminResources({
  currentUser,
  activeModuleKey,
  activeResource,
  activeColumns,
  activeAdminModule,
  run,
  onError: setError,
  onRoutesChanged: refreshRoutes,
  moduleViewState: activeModuleViewState,
  patchModuleViewState: patchActiveModuleViewState
})
```

Use `resourceState.activeRecords`, `resourceState.resourceDialog`, `resourceState.selectedResourceRow`, and `resourceState.resourceEditable` in place of the old state variables.

- [ ] **Step 3: Route all navigation actions through the navigation hook**

Replace:

```jsx
onSelectTopModule={handleSelectTopModule}
onSelectMenu={openMenuTag}
onSelectTag={setActiveModuleKey}
onCloseTag={handleCloseTag}
```

with:

```jsx
onSelectTopModule={(topId) => {
  const top = navigationTree.find((item) => item.id === topId)
  const first = firstMenuInTree(top?.children || [])
  if (first) openMenu(first)
}}
onSelectMenu={openMenu}
onSelectTag={selectTag}
onCloseTag={closeTag}
```

Use `tags={tags}` and `sideMenus={sideMenus.length > 0 ? sideMenus : fallbackSideMenus}` where `sideMenus` is calculated from `navigationTree`.

- [ ] **Step 4: Add unavailable and unknown route states**

Before specialized page rendering in `App.jsx`, add:

```jsx
{unavailableRoute && (
  <section className="module-dashboard hb-page-card">
    <header className="module-page-header">
      <div>
        <p className="page-breadcrumb">菜单不可用</p>
        <h1>{activeMenu?.name || '当前菜单'}</h1>
        <p>该菜单已被设置为停用，暂时不能打开。</p>
      </div>
    </header>
  </section>
)}
```

Wrap the normal module rendering with `!unavailableRoute`.

- [ ] **Step 5: Run App tests to verify route integration passes**

Run:

```powershell
npm.cmd test -- App.test.jsx --run
```

Expected: PASS or only text-label failures due existing encoding artifacts. If label failures occur, adjust tests to use stable roles/test ids rather than rewriting all copy.

- [ ] **Step 6: Commit router integration**

```powershell
git add -- heartbeat-web/src/main.jsx heartbeat-web/src/App.jsx heartbeat-web/src/layout/AdminLayout.jsx heartbeat-web/src/layout/LayoutHeader.jsx heartbeat-web/src/layout/LayoutSider.jsx heartbeat-web/src/layout/TagsView.jsx
git commit -m "feat: wire admin shell to browser routes"
```

---

## Task 5: Resource View-State Persistence And Menu Refresh

**Files:**
- Modify: `heartbeat-web/src/components/admin/ResourceDialog.jsx`
- Modify: `heartbeat-web/src/components/admin/ResourceTable.jsx`
- Modify: `heartbeat-web/src/App.test.jsx`

- [ ] **Step 1: Add failing test for dialog draft preservation**

Append to `heartbeat-web/src/App.test.jsx`:

```jsx
test('preserves resource dialog draft while switching open tags', async () => {
  mockLoggedIn()
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
    if (url === '/api/v1/auth/me') return authMeResponse()
    if (url === '/api/v1/iam/routes') {
      return {
        ok: true,
        json: async () => ({
          code: '0',
          msg: 'success',
          data: [{
            id: 'system',
            name: '系统管理',
            type: 'DIR',
            children: [
              {id: 'system-user', name: '用户管理', type: 'MENU', path: '/system/user', permission: 'system:user:list'},
              {id: 'system-role', name: '角色管理', type: 'MENU', path: '/system/role', permission: 'system:role:list'}
            ]
          }]
        })
      }
    }
    if (url === '/api/v1/admin/resources/users') return {ok: true, json: async () => ({code: '0', data: []})}
    if (url === '/api/v1/admin/resources/roles') return {ok: true, json: async () => ({code: '0', data: []})}
    return adminModulesResponse()
  })

  render(<App />)

  await userEvent.click(await screen.findByRole('button', {name: /用户管理/}))
  await userEvent.click(screen.getByRole('button', {name: /新增/}))
  await userEvent.type(screen.getByLabelText(/用户/), 'alice')
  await userEvent.click(screen.getByRole('button', {name: /角色管理/}))
  await userEvent.click(screen.getByRole('button', {name: /用户管理/}))

  expect(screen.getByDisplayValue('alice')).toBeInTheDocument()
})
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
npm.cmd test -- App.test.jsx --run
```

Expected: FAIL because `ResourceDialog` draft is reset when switching modules.

- [ ] **Step 3: Make ResourceDialog draft-controllable**

Modify `heartbeat-web/src/components/admin/ResourceDialog.jsx` signature:

```js
export default function ResourceDialog({ open, mode, resource, values, draftValues, onDraftChange, onClose, onSubmit }) {
```

Change state initialization and effect:

```js
const [formValues, setFormValues] = useState(draftValues || values || definition.emptyValues)

useEffect(() => {
  setFormValues(draftValues || values || definition.emptyValues)
}, [draftValues, values, resource])
```

Change `updateValue`:

```js
function updateValue(name, value) {
  setFormValues((previous) => {
    const next = {...previous, [name]: value}
    onDraftChange?.(next)
    return next
  })
}
```

Pass from `App.jsx`:

```jsx
<ResourceDialog
  open={resourceDialog.open}
  mode={resourceDialog.mode}
  resource={activeResource}
  values={resourceFormValues}
  draftValues={activeModuleViewState.dialogDraft}
  onDraftChange={resourceState.onDialogDraftChange}
  onClose={resourceState.closeResourceDialog}
  onSubmit={resourceState.submitResourceDialog}
/>
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
npm.cmd test -- App.test.jsx --run
```

Expected: PASS.

- [ ] **Step 5: Commit resource view-state persistence**

```powershell
git add -- heartbeat-web/src/components/admin/ResourceDialog.jsx heartbeat-web/src/App.jsx heartbeat-web/src/App.test.jsx
git commit -m "feat: preserve admin resource view state"
```

---

## Task 6: Appearance Settings And Glass Performance

**Files:**
- Modify: `heartbeat-web/src/layout/LayoutHeader.jsx`
- Modify: `heartbeat-web/src/components/AppearanceSettingsPanel/AppearanceSettingsPanel.jsx`
- Modify: `heartbeat-web/src/components/AppearanceSettingsPanel/AppearanceSettingsPanel.module.css`
- Modify: `heartbeat-web/src/theme/heartbeat-admin.css`
- Modify: `heartbeat-web/src/styles.css`
- Modify: `heartbeat-web/src/App.test.jsx`

- [ ] **Step 1: Add failing test for header appearance control**

Update the existing appearance test in `heartbeat-web/src/App.test.jsx` so clicking the header button reveals appearance controls:

```jsx
await userEvent.click(await screen.findByRole('button', { name: '主题与视觉效果' }))
expect(await screen.findByRole('radiogroup', { name: '界面风格' })).toBeInTheDocument()
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
npm.cmd test -- App.test.jsx --run
```

Expected: FAIL because the header button is inert.

- [ ] **Step 3: Make appearance panel controllable**

Modify `AppearanceSettingsPanel.jsx`:

```js
export default function AppearanceSettingsPanel({
  colorMode,
  onColorModeChange,
  accentColor,
  onAccentColorChange,
  visualStyle,
  onVisualStyleChange,
  fluidEnabled,
  onFluidChange,
  syncState,
  className = '',
  open: controlledOpen,
  onOpenChange
}) {
  const [internalOpen, setInternalOpen] = useState(false)
  const open = controlledOpen ?? internalOpen
  function setOpen(next) {
    if (onOpenChange) onOpenChange(next)
    else setInternalOpen(next)
  }
```

Keep the button handler:

```jsx
onClick={() => setOpen(!open)}
```

In `App.jsx`, add:

```js
const [appearanceSettingsOpen, setAppearanceSettingsOpen] = useState(false)
```

Pass to `MobileAdminShell`/desktop settings location if needed, and to `AdminLayout`:

```jsx
onAppearanceSettingsOpen={() => setAppearanceSettingsOpen(true)}
appearanceSettingsOpen={appearanceSettingsOpen}
onAppearanceSettingsOpenChange={setAppearanceSettingsOpen}
```

Modify `LayoutHeader.jsx` button:

```jsx
<button type="button" onClick={onAppearanceSettingsOpen} disabled={Boolean(busy)}>
  主题与视觉效果
</button>
```

- [ ] **Step 4: Add glass performance CSS**

In `heartbeat-web/src/theme/heartbeat-admin.css`, add to major glass surfaces:

```css
.hb-layout-header,
.hb-layout-sider,
.hb-tags-view,
.hb-page-card,
.hb-stat-card,
.module-toolbar,
.panel {
  transform: translateZ(0);
}
```

Add restrained dense-page handling:

```css
.hb-admin-layout.liquid-glass-enabled[data-glass-mode="restrained"] .resource-table-wrap,
.hb-admin-layout.liquid-glass-enabled[data-glass-mode="restrained"] .resource-table {
  background: rgba(255, 255, 255, 0.92);
}

.hb-admin-layout.liquid-glass-enabled[data-glass-mode="balanced"] .resource-table-wrap {
  background: rgba(255, 255, 255, 0.84);
}
```

Do not add global `will-change`.

- [ ] **Step 5: Run tests**

Run:

```powershell
npm.cmd test -- App.test.jsx themeService.test.js --run
```

Expected: PASS.

- [ ] **Step 6: Commit appearance and glass tuning**

```powershell
git add -- heartbeat-web/src/layout/LayoutHeader.jsx heartbeat-web/src/components/AppearanceSettingsPanel/AppearanceSettingsPanel.jsx heartbeat-web/src/components/AppearanceSettingsPanel/AppearanceSettingsPanel.module.css heartbeat-web/src/theme/heartbeat-admin.css heartbeat-web/src/styles.css heartbeat-web/src/App.jsx heartbeat-web/src/App.test.jsx
git commit -m "feat: polish glass appearance controls"
```

---

## Task 7: Final Verification And Build

**Files:**
- Modify if needed: tests touched by previous tasks

- [ ] **Step 1: Run focused frontend test suite**

Run:

```powershell
npm.cmd test -- --run
```

Expected: all Vitest tests pass.

- [ ] **Step 2: Run production build**

Run:

```powershell
npm.cmd run build
```

Expected: Vite build succeeds.

- [ ] **Step 3: Review changed files**

Run:

```powershell
git status --short
git diff --stat
```

Expected: only frontend implementation files and test files are modified. The pre-existing `heartbeat-start/src/main/resources/application.properties` remains unrelated and should not be staged.

- [ ] **Step 4: Commit final verification fixes if any**

If verification required small fixes:

```powershell
git add -- heartbeat-web
git commit -m "test: verify frontend glass routing integration"
```

If no files changed after previous commits, do not create an empty commit.

---

## Self-Review Notes

- Spec coverage: routing, backend route/menu integration, hidden/disabled menu behavior, tag persistence, resource state, App decomposition, appearance controls, glass performance, and verification all map to tasks.
- DDD boundaries: pure domain rules live in `domain/admin`, browser storage is infrastructure, workflows live in application hooks, and the React shell composes those units.
- TDD: each behavior task starts with failing tests before implementation.
- YAGNI: no Zustand or keep-alive library is added up front; the plan uses Context/hooks and session storage first.
