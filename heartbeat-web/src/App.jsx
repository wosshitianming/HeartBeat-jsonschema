import {lazy, Suspense, useCallback, useEffect, useMemo, useState} from 'react'
import {useLocation, useNavigate} from 'react-router-dom'
import {adminApi, authApi, iamApi, structureApi, toolApi} from './api'
import {
  buildResourcePayload,
  columnsForResource,
  firstMenuInTree,
  flattenMenuRows,
  flattenRouteModules,
  isResourceReadOnly,
  recordFromResource,
  resolveTopModuleId,
  resolveTopModuleIdForPath,
  resourceFromModule,
  sideMenusForTop,
  splitTopSideMenus,
  toResourceFormValues
} from './application/admin/adminModuleService'
import {appPathForMenu, findMenuByAppPath, findMenuById} from './domain/admin/navigationPolicy'
import {detectBackdropSupport, effectiveSurfaceMode} from './domain/admin/performancePolicy'
import {DEFAULT_ADMIN_PATH, tagKeyForLocation, upsertTagForLocation} from './domain/admin/workspaceRouting'
import {readWorkspaceState, writeWorkspaceState} from './infrastructure/browser/workspaceStorage'
import ResourceDialog from './components/admin/ResourceDialog'
import RoleMenuDialog from './components/admin/RoleMenuDialog'
import ResourceTable from './components/admin/ResourceTable'
import FluidBackground from './components/FluidBackground/FluidBackground'
import AdminLayout from './layout/AdminLayout'
import useAppearanceTheme from './appearance/useAppearanceTheme'
import {safeStorageGet, safeStorageRemove, safeStorageSet} from './infrastructure/browser/safeStorage'
import './styles.css'
import './theme/heartbeat-admin.css'

const SchemaForm = lazy(() => import('./components/SchemaForm'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const PayCashierPage = lazy(() => import('./pages/pay/PayCashierPage'))
const FlowStudioPage = lazy(() => import('./pages/flow/FlowStudioPage'))
const CodeGenPage = lazy(() => import('./pages/tool/CodeGenPage'))
const ServerMonitorPage = lazy(() => import('./pages/monitor/ServerMonitorPage'))

function LazyModuleFallback({label = 'Loading module...'}) {
    return (
        <div className="table-empty lazy-module-fallback" role="status" aria-live="polite">
            {label}
        </div>
    )
}

// ----- 示例数据 -----
const initialSamples = JSON.stringify([
  {
    name: 'Alice',
    age: 20,
    profile: { email: 'alice@example.com' },
    tags: ['vip']
  },
  {
    name: 'Bob',
    profile: { email: 'bob@example.com', nickname: 'B' },
    tags: ['new', 'trial']
  }
], null, 2)

const initialPayload = JSON.stringify({
  name: 'Charlie',
  age: 23,
  profile: { email: 'charlie@example.com' },
  extraFromVendor: true
}, null, 2)

// ----- 默认 UI 覆盖配置（JSON Path -> ui:title）-----
const defaultOverrides = JSON.stringify({
  "$.name": { "title": "姓名" },
  "$.age": { "title": "年龄" },
  "$.profile.email": { "title": "邮箱" },
  "$.profile.nickname": { "title": "昵称" },
  "$.tags": { "title": "标签" }
}, null, 2)

// ----- 通用工具 -----
function parseJson(text, label) {
  try {
    return JSON.parse(text)
  } catch (error) {
      throw new Error(`${label}不是有效的 JSON：${error.message}`)
  }
}

function rememberSessionUser(user) {
  if (!user?.id) return
  try {
      const session = JSON.parse(safeStorageGet('heartbeat_admin_session') || '{}')
      safeStorageSet('heartbeat_admin_session', JSON.stringify({
      ...session,
      userId: user.id
    }))
  } catch {
      safeStorageSet('heartbeat_admin_session', JSON.stringify({userId: user.id}))
  }
}

function saveAuthSession(result) {
    const tokens = result?.tokens || result || {}
    const user = result?.user?.fields || result?.user
    const tenantId = tokens.tenantId || user?.tenantId || safeStorageGet('heartbeat_tenant_id') || '1'
    safeStorageSet('heartbeat_tenant_id', String(tenantId))
    safeStorageSet('heartbeat_admin_session', JSON.stringify({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        tenantId,
        userId: user?.id
  }))
}

function authResultUser(result) {
    return result?.user?.fields || result?.user || null
}

const COMMON_FIELD_TITLES = {
  id: 'ID',
  name: '姓名',
  user_name: '用户名',
  username: '用户名',
  nick_name: '昵称',
  nickname: '昵称',
  age: '年龄',
  gender: '性别',
  sex: '性别',
  email: '邮箱',
  phone: '手机号',
  mobile: '手机号',
  telephone: '电话',
  address: '地址',
  city: '城市',
  province: '省份',
  country: '国家',
  profile: '资料',
  avatar: '头像',
  tags: '标签',
  tag: '标签',
  status: '状态',
  type: '类型',
  code: '编码',
  title: '标题',
  description: '描述',
  created_at: '创建时间',
  updated_at: '更新时间'
}

function isPlainObject(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function normalizeFieldKey(key) {
  return String(key)
      .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
      .replace(/[\s-]+/g, '_')
      .toLowerCase()
      .replace(/\d+$/g, '')
}

function inferChineseTitle(key) {
  const normalized = normalizeFieldKey(key)
  if (COMMON_FIELD_TITLES[normalized]) {
    return COMMON_FIELD_TITLES[normalized]
  }

  const segments = normalized.split('_').filter(Boolean)
  if (segments.length > 1 && segments.every((segment) => COMMON_FIELD_TITLES[segment])) {
    return segments.map((segment) => COMMON_FIELD_TITLES[segment]).join('')
  }

  return null
}

function mapWidget(widget) {
  if (!widget) return null
  if (widget === 'switch') return 'checkbox'
  if (widget === 'fieldset' || widget === 'list' || widget === 'number') return null
  return widget
}

function normalizeFieldConfig(config = {}, fieldName) {
  const result = {}
  const title = config['ui:title'] ?? config.title
  if (title && title !== fieldName) {
    result['ui:title'] = title
  }

  const placeholder = config['ui:placeholder'] ?? config.placeholder
  if (placeholder) {
    result['ui:placeholder'] = placeholder
  }

  const help = config['ui:help'] ?? config.help ?? config.description
  if (help) {
    result['ui:help'] = help
  }

  const widget = mapWidget(config['ui:widget'] ?? config.widget)
  if (widget) {
    result['ui:widget'] = widget
  }
  if (config.hidden === true || config.display === false) {
    result['ui:widget'] = 'hidden'
  }

  const order = config['ui:order'] ?? config.order
  if (Array.isArray(order)) {
    result['ui:order'] = order
  }

  return result
}

function mergeUiSchemas(...schemas) {
  return schemas.reduce((merged, schema) => {
    if (!isPlainObject(schema)) return merged
    Object.entries(schema).forEach(([key, value]) => {
      if (isPlainObject(value) && isPlainObject(merged[key])) {
        merged[key] = mergeUiSchemas(merged[key], value)
      } else {
        merged[key] = value
      }
    })
    return merged
  }, {})
}

function JsonPanel({ value, editable = false, onChange, label }) {
  const text = typeof value === 'string' ? value : JSON.stringify(value ?? {}, null, 2)
  if (editable) {
    return (
        <textarea
            className="code-editor result-editor"
            aria-label={label}
            value={text}
            onChange={(event) => onChange(event.target.value)}
            spellCheck="false"
        />
    )
  }
  return <pre className="code-output">{text}</pre>
}

/**
 * 将 JSON Path 覆盖配置转换为 RJSF 的 uiSchema。
 * 同时兼容 title/widget 与 RJSF 的 ui:title/ui:widget。
 */
function buildUiSchemaFromOverrides(overrides) {
  const result = {}
  Object.entries(overrides).forEach(([path, config]) => {
    const keys = path
        .replace(/^\$\.?/, '')
        .split('.')
        .filter(Boolean)

    let current = result
    let fieldName = ''
    for (let i = 0; i < keys.length; i++) {
      const isLast = i === keys.length - 1
      const isArrayItem = keys[i].endsWith('[]')
      const key = keys[i].replace(/\[\]$/, '')
      fieldName = key

      if (!current[key] || typeof current[key] !== 'object') {
        current[key] = {}
      }

      if (isArrayItem) {
        if (!current[key].items || typeof current[key].items !== 'object') {
          current[key].items = {}
        }
        current = current[key].items
        if (isLast) {
          Object.assign(current, normalizeFieldConfig(config, 'items'))
        }
        continue
      }

      if (isLast) {
        current[key] = mergeUiSchemas(current[key], normalizeFieldConfig(config, fieldName))
      } else {
        current = current[key]
      }
    }

    if (keys.length === 0) {
      Object.assign(result, normalizeFieldConfig(config, '$'))
    }
  })
  return result
}

function buildUiSchemaFromNeutral(neutralUiSchema, fieldName = '$') {
  if (!isPlainObject(neutralUiSchema)) return {}

  const result = normalizeFieldConfig(neutralUiSchema, fieldName)
  if (isPlainObject(neutralUiSchema.fields)) {
    Object.entries(neutralUiSchema.fields).forEach(([key, child]) => {
      result[key] = buildUiSchemaFromNeutral(child, key)
    })
  }

  if (isPlainObject(neutralUiSchema.items)) {
    result.items = buildUiSchemaFromNeutral(neutralUiSchema.items, 'items')
  }

  return result
}

function buildInferredTitleUiSchema(schema) {
  if (!isPlainObject(schema)) return {}

  const result = {}
  if (isPlainObject(schema.properties)) {
    Object.entries(schema.properties).forEach(([key, propertySchema]) => {
      const child = buildInferredTitleUiSchema(propertySchema)
      const title = propertySchema.title || inferChineseTitle(key)
      result[key] = title
          ? mergeUiSchemas(child, { 'ui:title': title })
          : child
    })
  }

  if (isPlainObject(schema.items)) {
    result.items = buildInferredTitleUiSchema(schema.items)
  }

  return result
}

function collectTitleOverridesFromValue(value, path, target) {
  if (Array.isArray(value)) {
    value.forEach((item) => collectTitleOverridesFromValue(item, `${path}[]`, target))
    return
  }

  if (!isPlainObject(value)) return

  Object.entries(value).forEach(([key, child]) => {
    const fieldPath = path === '$' ? `$.${key}` : `${path}.${key}`
    const title = inferChineseTitle(key)
    if (title) {
      target[fieldPath] = { title }
    }
    collectTitleOverridesFromValue(child, fieldPath, target)
  })
}

function generateTitleOverrides(samples) {
  const result = {}
  samples.forEach((sample) => collectTitleOverridesFromValue(sample, '$', result))
  return result
}

function collectSchemaFields(schema, path = '$', result = []) {
  if (!isPlainObject(schema) || !isPlainObject(schema.properties)) {
    return result
  }

  Object.entries(schema.properties).forEach(([key, child]) => {
    const fieldPath = path === '$' ? `$.${key}` : `${path}.${key}`
    const type = Array.isArray(child.type) ? child.type.join(' | ') : child.type || 'object'
    result.push({
      path: fieldPath,
      name: key,
      type,
      title: child.title || inferChineseTitle(key) || key
    })
    collectSchemaFields(child, fieldPath, result)
    if (isPlainObject(child.items)) {
      collectSchemaFields(child.items, `${fieldPath}[]`, result)
    }
  })
  return result
}

// ----- 后台模块配置 -----
const TABS = [
  { key: 'JSON_SCHEMA', label: 'JSON Schema' },
  { key: 'UI_SCHEMA', label: 'UI Schema' },
  { key: 'STRUCTURE_MODEL', label: '结构模型' },
  { key: 'FORM', label: '表单预览' }
]

const fallbackAdminModules = [
  {
    key: 'structure',
    name: '结构展示配置',
    category: '数据配置',
    description: '从 JSON 样例推断 JSON Schema、UI Schema 并管理版本。',
    permissionPrefix: 'structure:definition:list',
    status: '结构工作台',
    resource: null,
    actions: [],
    metrics: [],
    columns: [],
    records: []
  },
  {
    key: 'system-user',
    name: '用户管理',
    category: '系统管理',
    description: '系统操作者账号、部门与角色分配。',
    permissionPrefix: 'system:user:list',
    status: '已接入',
    resource: 'users',
    actions: ['新增', '修改', '删除', '刷新', '导出'],
    metrics: [],
    columns: [],
    records: []
  },
  {
    key: 'system-menu',
    name: '菜单管理',
    category: '系统管理',
    description: '维护左侧导航、路由、图标与权限标识。',
    permissionPrefix: 'system:menu:list',
    status: '已接入',
    resource: 'menus',
    actions: ['新增', '修改', '删除', '刷新', '导出'],
    metrics: [],
    columns: [],
    records: []
  },
  {
    key: 'system-role',
    name: '角色管理',
    category: '系统管理',
    description: '角色权限与数据范围配置。',
    permissionPrefix: 'system:role:list',
    status: '已接入',
    resource: 'roles',
    actions: ['新增', '修改', '删除', '分配菜单', '刷新', '导出'],
    metrics: [],
    columns: [],
    records: []
  },
  {
    key: 'system-config',
    name: '参数管理',
    category: '系统管理',
    description: '系统运行参数键值维护。',
    permissionPrefix: 'system:config:list',
    status: '已接入',
    resource: 'configs',
    actions: ['新增', '修改', '删除', '刷新', '导出'],
    metrics: [],
    columns: [],
    records: []
  }
]

const fallbackSideMenus = [
  {
    id: 'system',
    name: '系统管理',
    type: 'DIR',
    children: [
      { id: 'system-user', name: '用户管理', type: 'MENU', permission: 'system:user:list' },
      { id: 'system-menu', name: '菜单管理', type: 'MENU', permission: 'system:menu:list' },
      { id: 'system-role', name: '角色管理', type: 'MENU', permission: 'system:role:list' }
    ]
  },
  {
    id: 'data',
    name: '数据配置',
    type: 'DIR',
    children: [
      { id: 'structure', name: '结构展示配置', type: 'MENU', permission: 'structure:definition:list' },
      { id: 'flow', name: '流程编排', type: 'MENU', permission: 'flow:studio:list' }
    ]
  },
    {
        id: 'monitor',
        name: '运维监控',
        type: 'DIR',
        children: [
            {id: 'monitor-server', name: '服务监控', type: 'MENU', permission: 'monitor:server:list'}
        ]
    },
    {
        id: 'tool',
        name: '基础工具',
        type: 'DIR',
        children: [
            {id: 'tool-gen', name: '代码生成', type: 'MENU', permission: 'tool:gen:list'}
        ]
  }
]

function tagFromLegacyMenu(tag) {
    if (tag.key && tag.path) return tag
    const menu = {id: tag.id, name: tag.name}
    const path = appPathForMenu(menu)
    return {
        ...tag,
        id: path,
        key: path,
        path,
        menuId: tag.id,
        title: tag.title || tag.name,
        closable: tag.closable ?? tag.id !== 'structure'
    }
}

function normalizeWorkspaceTags(tags = []) {
    return tags.map(tagFromLegacyMenu)
}

function menuIdFromAdminPath(pathname = '') {
    const match = pathname.match(/^\/admin\/module\/([^/?#]+)$/)
    return match ? decodeURIComponent(match[1]) : null
}

// ----- 主应用 -----
export default function App() {
    const location = useLocation()
    const navigate = useNavigate()
    const activeTagKey = tagKeyForLocation(location)
  const [currentUser, setCurrentUser] = useState(null)
    const [loginForm, setLoginForm] = useState({
        tenantId: safeStorageGet('heartbeat_tenant_id') || '1',
        username: 'admin',
        password: ''
    })
  const [socialProviders, setSocialProviders] = useState([])
  const [pendingBind, setPendingBind] = useState(null)
  const [authChecked, setAuthChecked] = useState(false)
    const [name, setName] = useState('用户资料结构')
    const [description, setDescription] = useState('由用户样例推断的结构定义')
  const [samplesText, setSamplesText] = useState(initialSamples)
  const [mode, setMode] = useState('LENIENT')
  const [overridesText, setOverridesText] = useState(defaultOverrides)
  const [preview, setPreview] = useState(null)
  const [activeTab, setActiveTab] = useState('JSON_SCHEMA')
  const [definitions, setDefinitions] = useState([])
  const [selected, setSelected] = useState(null)
  const [payloadText, setPayloadText] = useState(initialPayload)
  const [validation, setValidation] = useState(null)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [formData, setFormData] = useState({})
  const [submittedData, setSubmittedData] = useState(null)
  const [diffResult, setDiffResult] = useState(null)
  const [adminModules, setAdminModules] = useState(fallbackAdminModules)
  const [routeTree, setRouteTree] = useState([])
    const [routeStatus, setRouteStatus] = useState('loading')
  const [activeTopModuleKey, setActiveTopModuleKey] = useState('data')
  const [openTags, setOpenTags] = useState([{ id: 'structure', name: '结构展示配置', closable: false }])
  const [activeModuleKey, setActiveModuleKey] = useState('structure')
    const [workspaceRestored, setWorkspaceRestored] = useState(false)
  const [moduleData, setModuleData] = useState({})
  const [resourceDialog, setResourceDialog] = useState({ open: false, mode: 'create', row: null })
  const [roleMenuDialog, setRoleMenuDialog] = useState({ open: false, role: null })
  const [selectedResourceRow, setSelectedResourceRow] = useState(null)
    const [surfaceEnvironment, setSurfaceEnvironment] = useState({
        supportsBackdrop: true,
        reducedMotion: false
    })
  const {
    colorMode,
    fluidEnabled,
    accentColor,
    visualStyle,
    colorScheme,
    changeColorMode,
    changeFluidEnabled,
    changeAccentColor,
      changeVisualStyle,
    syncState
  } = useAppearanceTheme(currentUser)

  const selectedDefinition = useMemo(
      () => definitions.find((item) => item.id === selected) || null,
      [definitions, selected]
  )
  const activeAdminModule = useMemo(
      () => adminModules.find((item) => item.key === activeModuleKey) || adminModules[0],
      [adminModules, activeModuleKey]
  )
  const activeResource = useMemo(
      () => resourceFromModule(activeAdminModule),
      [activeAdminModule]
  )
  const activeRecords = useMemo(() => {
    if (!activeResource) return []
    return moduleData[activeResource] || []
  }, [activeResource, moduleData])
  const resourceEditable = useMemo(
      () => Boolean(activeResource) && !isResourceReadOnly(activeResource),
      [activeResource]
  )
  const activeColumns = useMemo(
      () => columnsForResource(activeResource, activeAdminModule),
      [activeResource, activeAdminModule]
  )
    const navigationTree = useMemo(
        () => (routeTree.length > 0 ? routeTree : fallbackSideMenus),
        [routeTree]
    )
    const topModules = useMemo(() => splitTopSideMenus(navigationTree), [navigationTree])
    const sideMenus = useMemo(
        () => sideMenusForTop(navigationTree, activeTopModuleKey),
        [navigationTree, activeTopModuleKey]
    )
    const routeReady = routeStatus === 'ready' || routeStatus === 'fallback'
    const resolveMenuForPath = useCallback((pathname) => {
        const matchedMenu = findMenuByAppPath(navigationTree, pathname)
        if (matchedMenu) return matchedMenu

        const menuId = menuIdFromAdminPath(pathname)
        if (menuId) {
            const menu = findMenuById(navigationTree, menuId)
            if (menu) return menu
            const module = adminModules.find((item) => item.key === menuId)
            if (module) {
                return {
                    id: module.key,
                    name: module.name,
                    path: module.appPath || `/admin/module/${encodeURIComponent(module.key)}`,
                    type: 'MENU'
                }
            }
        }

        if (pathname === '/' || pathname === '/admin') {
            return findMenuById(navigationTree, 'structure') || {
                id: 'structure',
                name: '结构展示配置',
                type: 'MENU'
            }
        }

        return null
    }, [adminModules, navigationTree])
  const structureMode = false
    const requestedGlassMode = visualStyle === 'glass' && fluidEnabled ? 'balanced' : 'flat'
    const glassMode = requestedGlassMode === 'flat'
        ? 'flat'
        : effectiveSurfaceMode({
            manualMode: requestedGlassMode,
            supportsBackdrop: surfaceEnvironment.supportsBackdrop,
            reducedMotion: surfaceEnvironment.reducedMotion,
            rowCount: activeRecords.length
        })
  const liquidGlassEnabled = glassMode !== 'flat'
    const fluidBackgroundEnabled = liquidGlassEnabled && glassMode !== 'restrained'

    useEffect(() => {
        const media = window.matchMedia?.('(prefers-reduced-motion: reduce)')
        const update = () => {
            setSurfaceEnvironment({
                supportsBackdrop: detectBackdropSupport(window),
                reducedMotion: Boolean(media?.matches)
            })
        }
        update()
        media?.addEventListener?.('change', update)
        return () => {
            media?.removeEventListener?.('change', update)
        }
    }, [])

    useEffect(() => {
        if (currentUser?.id && !workspaceRestored) return
        const userId = currentUser?.id || 'anonymous'
        writeWorkspaceState(userId, {
            activeKey: activeTagKey,
            tags: openTags
        })
    }, [activeTagKey, currentUser?.id, openTags, workspaceRestored])

    useEffect(() => {
        if (!currentUser?.id) {
            setWorkspaceRestored(false)
            return
        }
        const cached = readWorkspaceState(currentUser.id)
        if (cached?.tags?.length) {
            setOpenTags(normalizeWorkspaceTags(cached.tags))
        }
        setWorkspaceRestored(true)
    }, [currentUser?.id])

  useEffect(() => {
    let mounted = true
      const saved = safeStorageGet('heartbeat_admin_session')
    if (!saved) {
      setAuthChecked(true)
      return () => {
        mounted = false
      }
    }
    authApi.me()
        .then((user) => {
          if (mounted) {
            rememberSessionUser(user)
            setCurrentUser(user)
          }
        })
        .catch(() => {
            safeStorageRemove('heartbeat_admin_session')
        })
        .finally(() => {
          if (mounted) setAuthChecked(true)
        })
    return () => {
      mounted = false
    }
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
    return () => {
      mounted = false
    }
  }, [authChecked, currentUser])

  useEffect(() => {
      if (!authChecked || !currentUser?.id) {
          setRouteTree([])
          setAdminModules(fallbackAdminModules)
          setRouteStatus('loading')
          return undefined
      }

    let mounted = true
      const controller = new AbortController()
      setRouteStatus('loading')

      async function loadRoutes() {
          try {
              const routes = await iamApi.routes({signal: controller.signal})
              if (!mounted) return
              const safeRoutes = Array.isArray(routes) ? routes : []
              const modules = flattenRouteModules(safeRoutes)
              setRouteTree(safeRoutes)
              setAdminModules(modules.length > 0 ? modules : fallbackAdminModules)
              setRouteStatus('ready')
          } catch (routeError) {
              if (routeError?.name === 'AbortError') return
              try {
                  const items = await adminApi.modules({signal: controller.signal})
                  if (!mounted) return
                  const safeItems = Array.isArray(items) ? items : []
                  const modules = safeItems.some((item) => item.key === 'structure')
                      ? safeItems
                      : [fallbackAdminModules[0], ...safeItems]
                  setRouteTree([])
                  setAdminModules(modules.length > 0 ? modules : fallbackAdminModules)
                  setRouteStatus('fallback')
              } catch (fallbackError) {
                  if (fallbackError?.name === 'AbortError' || !mounted) return
                  setRouteTree([])
                  setAdminModules(fallbackAdminModules)
                  setRouteStatus('fallback')
              }
          }
      }

      loadRoutes()
    return () => {
      mounted = false
        controller.abort()
    }
  }, [authChecked, currentUser?.id])

  useEffect(() => {
    if (!currentUser || activeModuleKey === 'structure' || !activeResource) return
      const controller = new AbortController()
      loadModuleResource(activeResource, {signal: controller.signal}).catch((err) => {
          if (err?.name !== 'AbortError') {
              setError(err.message || 'Resource loading failed')
          }
      })
    setSelectedResourceRow(null)
      return () => {
          controller.abort()
      }
  }, [currentUser, activeModuleKey, activeResource])

    useEffect(() => {
        if (!currentUser || !routeReady) return
        if (location.pathname === '/' || location.pathname === '/admin') {
            navigate(DEFAULT_ADMIN_PATH, {replace: true})
            return
        }

        const menu = resolveMenuForPath(location.pathname)
        if (!menu) {
            setError('Unknown route')
            return
        }

        setError('')
        setActiveModuleKey(menu.id)
        const topId = resolveTopModuleIdForPath(navigationTree, location.pathname)
            || resolveTopModuleId(navigationTree, menu.id)
        if (topId) setActiveTopModuleKey(topId)
        setOpenTags((previous) => upsertTagForLocation(
            normalizeWorkspaceTags(previous),
            location,
            {
                menuId: menu.id,
                name: menu.name,
                title: menu.name,
                closable: menu.id !== 'structure'
            }
        ))
        setSelectedResourceRow(null)
    }, [
        currentUser,
        location,
        navigate,
        navigationTree,
        resolveMenuForPath,
        routeReady
    ])

    // UI 覆盖配置
  const uiOverrides = useMemo(() => {
    try {
      return JSON.parse(overridesText)
    } catch {
      return {}
    }
  }, [overridesText])

    // 构建生成请求
  function buildGenerationPayload() {
      const samples = parseJson(samplesText, 'JSON 样例')
    if (!Array.isArray(samples) || samples.length === 0) {
        throw new Error('JSON 样例必须是非空数组')
    }
      const uiOverrides = parseJson(overridesText, 'UI 覆盖配置')
    return { samples, validationMode: mode, uiOverrides }
  }

  function buildDraftPayload() {
    const payload = buildGenerationPayload()
    return {
      samples: payload.samples,
      validationMode: payload.validationMode,
      fieldOverrides: payload.uiOverrides
    }
  }

  async function run(action, work) {
    setBusy(action)
    setError('')
    try {
      return await work()
    } catch (err) {
        setError(err.message || '操作失败')
      return null
    } finally {
      setBusy('')
    }
  }

  async function handleLogin(event) {
    event.preventDefault()
    await run('login', async () => {
      const result = await authApi.login(loginForm)
      saveAuthSession(result)
        setCurrentUser(authResultUser(result))
      setPendingBind(null)
    })
  }

  async function handleSocialLogin(provider) {
    await run(`social-${provider}`, async () => {
      const auth = await authApi.socialAuthorize(provider)
      if (provider.toUpperCase() === 'MOCK') {
        const result = await authApi.socialCallback(provider, 'mock:demo', auth.state)
        if (result.status === 'PENDING_BIND') {
          setPendingBind(result)
          return
        }
        saveAuthSession(result)
          setCurrentUser(authResultUser(result))
        setPendingBind(null)
        return
      }
      window.location.href = auth.authorizeUrl
    })
  }

  async function handleSocialBind(event) {
    event.preventDefault()
    if (!pendingBind?.bindTicket) return
    await run('social-bind', async () => {
      const result = await authApi.socialBind({
        bindTicket: pendingBind.bindTicket,
        username: loginForm.username,
        password: loginForm.password
      })
      saveAuthSession(result)
        setCurrentUser(authResultUser(result))
      setPendingBind(null)
    })
  }

  async function handleLogout() {
    await run('logout', async () => {
        try {
            await authApi.logout()
        } finally {
            safeStorageRemove('heartbeat_admin_session')
            setCurrentUser(null)
        }
    })
  }

  function openMenuTag(menu) {
    if (!menu?.id) return
      navigate(appPathForMenu(menu))
  }

  function handleSelectTopModule(topModuleId) {
    setActiveTopModuleKey(topModuleId)
      const menus = sideMenusForTop(navigationTree, topModuleId)
    const firstMenu = firstMenuInTree(menus)
    if (firstMenu) openMenuTag(firstMenu)
  }

  function handleCloseTag(tagId) {
    setOpenTags((previous) => {
        const normalized = normalizeWorkspaceTags(previous)
        const closingTag = normalized.find((tag) => tag.key === tagId || tag.id === tagId)
        if (!closingTag || closingTag.closable === false) return normalized
        const next = normalized.filter((tag) => tag !== closingTag)
        if ((closingTag.key || closingTag.id) === activeTagKey) {
            const currentIndex = normalized.indexOf(closingTag)
            const target = next[Math.max(0, currentIndex - 1)] || next[currentIndex] || next[0]
            navigate(target?.path || DEFAULT_ADMIN_PATH, {replace: true})
      }
      return next
    })
  }

    function handleSelectTag(tag) {
        const target = typeof tag === 'string'
            ? normalizeWorkspaceTags(openTags).find((item) => item.key === tag || item.id === tag)
            : tag
        if (target?.path) {
            navigate(target.path)
        }
    }

    async function loadModuleResource(resource = activeResource, options = {}) {
    if (!resource) return []
        const items = resource === 'menus' ? await iamApi.menus(options) : await adminApi.resources(resource, options)
    const rows = resource === 'menus'
        ? flattenMenuRows(items)
        : items.map((item) => recordFromResource(resource, item))
    setModuleData((previous) => ({ ...previous, [resource]: rows }))
    return rows
  }

  async function handleJobAction(action) {
    if (!selectedResourceRow?.id) {
        setError('请先选择任务')
      return
    }
    const jobId = selectedResourceRow.id
    await run(`job-${action}`, async () => {
      if (action === 'run') await toolApi.runJob(jobId)
      if (action === 'pause') await toolApi.pauseJob(jobId)
      if (action === 'resume') await toolApi.resumeJob(jobId)
      if (action === 'refresh') await toolApi.refreshJobs()
      await loadModuleResource('jobs')
    })
  }

  async function handleModuleAction(action) {
      if (action.includes('刷新')) {
      if (!activeResource) {
          setError('当前模块未绑定资源接口')
        return
      }
      await run('module-refresh', () => loadModuleResource(activeResource))
      return
    }
    if (!activeResource) {
        setError('当前模块未绑定资源接口')
      return
    }
    if (isResourceReadOnly(activeResource)) {
        if (action.includes('导出')) {
        await run(`module-${action}`, async () => {
          const rows = await loadModuleResource(activeResource)
          exportRows(activeAdminModule.name, rows)
        })
      }
      return
    }
      if (action.includes('新增') || action.includes('创建')) {
      openResourceDialog('create')
      return
    }
      if (action.includes('修改') || action.includes('编辑') || action.includes('配置')) {
      if (!selectedResourceRow) {
          setError('请先选择要修改的记录')
        return
      }
      openResourceDialog('edit', selectedResourceRow)
      return
    }
      if (action.includes('删除')) {
      if (!selectedResourceRow) {
          setError('请先选择要删除的记录')
        return
      }
      await deleteResourceRow(selectedResourceRow)
      return
    }
      if (action.includes('分配菜单')) {
      if (!selectedResourceRow) {
          setError('请先选择要分配菜单的角色')
        return
      }
      await openRoleMenuDialog(selectedResourceRow)
      return
    }
    await run(`module-${action}`, async () => {
      const rows = await loadModuleResource(activeResource)
        if (action.includes('导出')) exportRows(activeAdminModule.name, rows)
    })
  }

  function openResourceDialog(mode, row = null) {
    setError('')
    setResourceDialog({ open: true, mode, row })
  }

  function closeResourceDialog() {
    setResourceDialog({ open: false, mode: 'create', row: null })
  }

  async function openRoleMenuDialog(row) {
    setError('')
    await run('role-menu-load', async () => {
      const detail = await iamApi.roleMenus(row.id)
      setRoleMenuDialog({
        open: true,
        role: {
          roleId: row.id,
            roleName: row['角色名称'] || row.raw?.name || row.id,
          menuIds: detail.menuIds || [],
          menuTree: detail.menuTree || []
        }
      })
    })
  }

  function closeRoleMenuDialog() {
    setRoleMenuDialog({ open: false, role: null })
  }

  async function submitRoleMenuDialog(menuIds) {
    const roleId = roleMenuDialog.role?.roleId
    if (!roleId) return
    await run('role-menu-save', async () => {
      await iamApi.assignRoleMenus(roleId, menuIds)
      closeRoleMenuDialog()
    })
  }

  async function submitResourceDialog(values) {
    await run(`resource-${resourceDialog.mode}`, async () => {
      const payload = buildResourcePayload(activeResource, values, resourceDialog.mode)
      if (resourceDialog.mode === 'edit') {
        const id = resourceDialog.row?.id
        if (activeResource === 'menus') {
          await iamApi.updateMenu(id, payload)
        } else {
          await adminApi.updateResource(activeResource, id, payload)
        }
      } else if (activeResource === 'menus') {
        await iamApi.createMenu(payload)
      } else {
        await adminApi.createResource(activeResource, payload)
      }
      closeResourceDialog()
      await loadModuleResource(activeResource)
    })
  }

  async function deleteResourceRow(row) {
      if (typeof window.confirm === 'function' && !window.confirm(`确认删除“${row[activeColumns[0]] || row.id}”吗？`)) {
      return
    }
    await run('resource-delete', async () => {
      if (activeResource === 'menus') {
        await iamApi.deleteMenu(row.id)
      } else {
        await adminApi.deleteResource(activeResource, row.id)
      }
      setSelectedResourceRow(null)
      await loadModuleResource(activeResource)
    })
  }

  function exportRows(moduleName, rows) {
    const blob = new Blob([JSON.stringify(rows, null, 2)], { type: 'application/json;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${moduleName || 'module'}-${Date.now()}.json`
    link.click()
    URL.revokeObjectURL(url)
  }

  async function handlePreview() {
    await run('preview', async () => {
      const result = await structureApi.preview(buildGenerationPayload())
      setPreview(result)
      setSubmittedData(null)
      setFormData({})
      setActiveTab('JSON_SCHEMA')
    })
  }

  async function refreshDefinitions(selectId) {
    const items = await structureApi.list()
    setDefinitions(items)
    if (selectId) setSelected(selectId)
    return items
  }

  async function handleSave() {
    if (!name.trim()) {
        setError('请输入结构名称')
      return
    }
    await run('save', async () => {
      const result = await structureApi.create({
        name: name.trim(),
        description: description.trim(),
        ...buildGenerationPayload(),
        activate: true
      })
      await refreshDefinitions(result.id)
    })
  }

  async function handleNewVersion() {
    if (!selected) return
    await run('version', async () => {
      const result = await structureApi.createVersion(selected, buildGenerationPayload())
      await refreshDefinitions(result.id)
    })
  }

  async function handleSaveDraft() {
    if (!selected) {
        setError('请先选择结构定义')
      return
    }
    await run('draft', async () => {
      const result = await structureApi.saveDraft(selected, buildDraftPayload())
      await refreshDefinitions(result.id)
    })
  }

  async function handleVersionFromDraft() {
    if (!selected) return
    await run('version-from-draft', async () => {
      const result = await structureApi.createVersionFromDraft(selected)
      await refreshDefinitions(result.id)
      setDiffResult(null)
    })
  }

  async function handleCopyVersionToDraft(versionNo) {
    await run(`copy-draft-${versionNo}`, async () => {
      const result = await structureApi.copyVersionToDraft(selected, versionNo)
      await refreshDefinitions(result.id)
    })
  }

  async function handleDiff(params) {
    if (!selected) return
    await run('diff', async () => {
      const result = await structureApi.diff(selected, params)
      setDiffResult(result)
    })
  }

  async function handleActivate(versionNo) {
    await run(`activate-${versionNo}`, async () => {
      const result = await structureApi.activate(selected, versionNo)
      await refreshDefinitions(result.id)
    })
  }

  async function handleRefresh() {
    if (activeModuleKey !== 'structure' && activeResource) {
      await run('refresh', () => loadModuleResource(activeResource))
      return
    }
    await run('refresh', () => refreshDefinitions(selected))
  }

  async function handleValidate() {
    if (!selected) {
        setError('请先选择结构定义')
      return
    }
    await run('validate', async () => {
      const result = await structureApi.validate(selected, {
          payload: parseJson(payloadText, '待校验数据')
      })
      setValidation(result)
    })
  }

  function handleGenerateTitleOverrides() {
    try {
        const samples = parseJson(samplesText, 'JSON 样例')
      if (!Array.isArray(samples) || samples.length === 0) {
          throw new Error('JSON 样例必须是非空数组')
      }
        const currentOverrides = parseJson(overridesText, 'UI 覆盖配置')
      const generatedOverrides = generateTitleOverrides(samples)
      setOverridesText(JSON.stringify({
        ...generatedOverrides,
        ...currentOverrides
      }, null, 2))
      setError('')
    } catch (err) {
        setError(err.message || '生成中文标题失败')
    }
  }

  function updateFieldOverride(path, patch) {
    const nextOverrides = {
      ...uiOverrides,
      [path]: {
        ...(uiOverrides[path] || {}),
        ...patch
      }
    }
    setOverridesText(JSON.stringify(nextOverrides, null, 2))
  }

  const artifacts = preview?.artifacts || {}
  const warnings = preview?.warnings || []
  const jsonSchema = artifacts.JSON_SCHEMA
  const neutralUiSchema = artifacts.UI_SCHEMA
  const displayFields = useMemo(() => collectSchemaFields(jsonSchema), [jsonSchema])
  const onlineCount = definitions.filter((definition) => definition.activeVersionNo).length
  const draftCount = definitions.filter((definition) => definition.draft).length
  const versionCount = definitions.reduce((total, definition) => total + definition.versions.length, 0)

  const mergedUiSchema = useMemo(() => {
    return mergeUiSchemas(
        buildUiSchemaFromNeutral(neutralUiSchema),
        buildInferredTitleUiSchema(jsonSchema),
        buildUiSchemaFromOverrides(uiOverrides)
    )
  }, [jsonSchema, neutralUiSchema, uiOverrides])
  const resourceFormValues = useMemo(
      () => toResourceFormValues(activeResource, resourceDialog.row),
      [activeResource, resourceDialog.row]
  )

  if (!authChecked) {
    return (
        <div className="login-page">
          <div className="login-card">
            <span className="brand-orb" />
              <h1>正在启动 HeartBeat</h1>
              <p>正在检查登录状态...</p>
          </div>
        </div>
    )
  }

  if (!currentUser) {
    return (
        <div className="login-page">
          <form className="login-card" onSubmit={pendingBind ? handleSocialBind : handleLogin}>
            <span className="brand-orb" />
              <p className="eyebrow">HEARTBEAT ADMIN</p>
              <h1>{pendingBind ? '绑定账号' : '管理员登录'}</h1>
            <p>
              {pendingBind
                  ? `检测到 ${pendingBind.nickname || pendingBind.provider} 尚未绑定账号，请输入现有账号完成绑定。`
                  : '使用租户、用户名和密码登录管理控制台。'}
            </p>
            {error && <div className="error-banner" role="alert">{error}</div>}
            <label>
                租户 ID
                <input
                    aria-label="租户 ID"
                    inputMode="numeric"
                    pattern="[0-9]+"
                    autoComplete="off"
                    required
                    value={loginForm.tenantId}
                    onChange={(event) => {
                        safeStorageSet('heartbeat_tenant_id', event.target.value)
                        setLoginForm({...loginForm, tenantId: event.target.value})
                    }}
                />
            </label>
              <label>
                  用户名
              <input
                  aria-label="用户名"
                  autoComplete="username"
                  required
                  value={loginForm.username}
                  onChange={(event) => setLoginForm({ ...loginForm, username: event.target.value })}
              />
            </label>
            <label>
                密码
              <input
                  aria-label="密码"
                  type="password"
                  autoComplete="current-password"
                  required
                  value={loginForm.password}
                  onChange={(event) => setLoginForm({ ...loginForm, password: event.target.value })}
              />
            </label>
            <button className="button primary" type="submit" disabled={Boolean(busy)}>
              {pendingBind
                  ? (busy === 'social-bind' ? '绑定中...' : '绑定账号')
                  : (busy === 'login' ? '登录中...' : '登录')}
            </button>
            {pendingBind && (
                <button
                    className="button ghost"
                    type="button"
                    onClick={() => setPendingBind(null)}
                    disabled={Boolean(busy)}
                >
                    取消绑定
                </button>
            )}
            {!pendingBind && socialProviders.length > 0 && (
                <div className="social-login-row">
                    <span>其他登录方式</span>
                  {socialProviders.map((item) => (
                      <button
                          key={item.provider}
                          className="button ghost social-login-button"
                          type="button"
                          disabled={Boolean(busy)}
                          onClick={() => handleSocialLogin(item.provider)}
                      >
                          {busy === `social-${item.provider}` ? '跳转中...' : (item.name || item.provider)}
                      </button>
                  ))}
                </div>
            )}
          </form>
        </div>
    )
  }

    if (routeStatus === 'loading') {
        return (
            <>
                <FluidBackground
                    enabled={fluidBackgroundEnabled}
                    visualStyle={liquidGlassEnabled ? 'glass' : visualStyle}
                    accentColor={accentColor}
                    colorScheme={colorScheme}
                    reducedMotion={surfaceEnvironment.reducedMotion}
                />
                <div className="login-page">
                    <div className="login-card">
                        <span className="brand-orb"/>
                        <h1>HeartBeat</h1>
                        <p>Loading workspace routes...</p>
                    </div>
                </div>
            </>
        )
    }

  return (
      <>
        <FluidBackground
            enabled={fluidBackgroundEnabled}
            visualStyle={liquidGlassEnabled ? 'glass' : visualStyle}
            accentColor={accentColor}
            colorScheme={colorScheme}
            reducedMotion={surfaceEnvironment.reducedMotion}
        />
        <AdminLayout
            structureMode={structureMode}
            topModules={topModules}
            sideMenus={sideMenus}
            tags={normalizeWorkspaceTags(openTags)}
            activeTopModuleId={activeTopModuleKey}
            activeTagKey={activeTagKey}
            activeModuleKey={activeModuleKey}
            liquidGlassEnabled={liquidGlassEnabled}
            glassMode={glassMode}
            currentUser={currentUser}
            busy={busy}
            fluidEnabled={fluidEnabled}
            onFluidChange={changeFluidEnabled}
            colorMode={colorMode}
            onColorModeChange={changeColorMode}
            accentColor={accentColor}
            onAccentColorChange={changeAccentColor}
            visualStyle={visualStyle}
            onVisualStyleChange={changeVisualStyle}
            syncState={syncState}
            onSelectTopModule={handleSelectTopModule}
            onSelectMenu={openMenuTag}
            onSelectTag={handleSelectTag}
            onCloseTag={handleCloseTag}
            onRefresh={handleRefresh}
            onLogout={handleLogout}
        >
            <Suspense fallback={<LazyModuleFallback/>}>
                {activeModuleKey === 'home-dashboard' && (
                    <DashboardPage currentUser={currentUser}/>
                )}
                {activeModuleKey === 'biz-pay-cashier' && (
                    <PayCashierPage/>
                )}
                {activeModuleKey === 'flow' && (
                    <FlowStudioPage busy={busy} onBusy={setBusy} onError={setError}/>
                )}
                {activeModuleKey === 'tool-gen' && (
                    <CodeGenPage busy={busy} onBusy={setBusy} onError={setError}/>
                )}
                {activeModuleKey === 'monitor-server' && (
                    <ServerMonitorPage busy={busy} onBusy={setBusy} onError={setError}/>
                )}
            </Suspense>
          {activeModuleKey === 'structure' && (
                <>
        <header className="structure-page-header" id="structure-workbench">
          <div>
            <p className="page-breadcrumb">数据配置 / 结构展示配置</p>
            <h1>结构配置工作台</h1>
            <p>从 JSON 样例推断结构，配置字段展示，并通过草稿与版本流程稳定发布。</p>
          </div>
          <div className="structure-summary" aria-label="结构配置概览">
            <span><strong>{definitions.length}</strong>结构定义</span>
            <span><strong>{onlineCount}</strong>已上线</span>
            <span><strong>{draftCount}</strong>草稿中</span>
            <span><strong>{versionCount}</strong>版本</span>
          </div>
        </header>

        {error && <div className="error-banner" role="alert">{error}</div>}

        <section className="workspace-grid">
          <article className="panel editor-panel">
            <div className="panel-heading">
              <div>
                <span className="step">01</span>
                <h2>输入样例</h2>
              </div>
              <select value={mode} onChange={(event) => setMode(event.target.value)} aria-label="校验模式">
                <option value="LENIENT">宽松模式</option>
                <option value="STRICT">严格模式</option>
              </select>
            </div>

            <div className="field-row">
              <label>
                结构名称
                <input value={name} onChange={(event) => setName(event.target.value)} />
              </label>
              <label>
                描述
                <input value={description} onChange={(event) => setDescription(event.target.value)} />
              </label>
            </div>

            <label className="editor-label" htmlFor="samples-editor">JSON 样例数组</label>
            <textarea
                id="samples-editor"
                className="code-editor samples-editor"
                value={samplesText}
                onChange={(event) => setSamplesText(event.target.value)}
                spellCheck="false"
            />

            <details className="advanced">
              <summary>UI Schema 覆盖配置</summary>
              <p>按 JSON Path 覆盖标题、控件或占位符，例如：{`{"$.name":{"title":"姓名"}}`}。</p>
              <textarea
                  aria-label="UI Schema 覆盖配置"
                  className="code-editor override-editor"
                  value={overridesText}
                  onChange={(event) => setOverridesText(event.target.value)}
                  spellCheck="false"
              />
              <button type="button" className="text-button" onClick={handleGenerateTitleOverrides}>
                根据字段名生成中文标题
              </button>
            </details>

            {displayFields.length > 0 && (
                <details className="advanced" open>
                  <summary>参数展示工作台</summary>
                  <div className="field-workbench">
                    {displayFields.map((field) => {
                      const override = uiOverrides[field.path] || {}
                      const hidden = override.hidden === true || override.display === false
                      return (
                          <div className="field-config" key={field.path}>
                            <div>
                              <strong>{field.path}</strong>
                              <span>{field.type}</span>
                            </div>
                            <input
                                aria-label={`${field.path} 展示标题`}
                                value={override.title ?? override['ui:title'] ?? field.title}
                                onChange={(event) => updateFieldOverride(field.path, { title: event.target.value })}
                            />
                            <select
                                aria-label={`${field.path} 控件类型`}
                                value={override.widget ?? override['ui:widget'] ?? ''}
                                onChange={(event) => updateFieldOverride(field.path, { widget: event.target.value || undefined })}
                            >
                              <option value="">默认控件</option>
                              <option value="text">文本</option>
                              <option value="textarea">多行文本</option>
                              <option value="number">数字</option>
                              <option value="checkbox">开关</option>
                              <option value="date">日期</option>
                              <option value="email">邮箱</option>
                            </select>
                            <label className="inline-check">
                              <input
                                  type="checkbox"
                                  checked={!hidden}
                                  onChange={(event) => updateFieldOverride(field.path, {
                                    hidden: !event.target.checked,
                                    display: event.target.checked
                                  })}
                              />
                              展示
                            </label>
                          </div>
                      )
                    })}
                  </div>
                </details>
            )}

            <div className="actions">
              <button className="button primary" onClick={handlePreview} disabled={Boolean(busy)}>
                {busy === 'preview' ? '生成中…' : '预览生成'}
              </button>
              <button className="button secondary" onClick={handleSave} disabled={Boolean(busy)}>
                {busy === 'save' ? '保存中…' : '保存并启用 v1'}
              </button>
              {selected && (
                  <>
                    <button className="button ghost" onClick={handleSaveDraft} disabled={Boolean(busy)}>
                      {busy === 'draft' ? '草稿保存中…' : '保存草稿'}
                    </button>
                    <button className="button ghost" onClick={handleNewVersion} disabled={Boolean(busy)}>
                      直接保存新版本
                    </button>
                    {selectedDefinition?.draft && (
                        <button className="button secondary" onClick={handleVersionFromDraft} disabled={Boolean(busy)}>
                          草稿保存为版本
                        </button>
                    )}
                  </>
              )}
            </div>
          </article>

          <article className="panel result-panel">
            <div className="panel-heading">
              <div>
                <span className="step">02</span>
                <h2>查看产物</h2>
              </div>
              <span className="status-pill">{preview ? '已生成' : '等待样例'}</span>
            </div>

            <div className="tabs">
              {TABS.map(({ key, label }) => (
                  <button
                      key={key}
                      className={activeTab === key ? 'tab active' : 'tab'}
                      onClick={() => setActiveTab(key)}
                  >
                    {label}
                  </button>
              ))}
            </div>

            {!preview ? (
                <div className="empty-state">
                  <span>{'{ }'}</span>
                  <p>点击“预览生成”，结构会在这里展开。</p>
                </div>
            ) : (
                <>
                  {activeTab === 'FORM' ? (
                      <div className="form-wrapper">
                          <Suspense fallback={<LazyModuleFallback label="Loading form renderer..."/>}>
                              <SchemaForm
                                  schema={artifacts.JSON_SCHEMA}
                                  uiSchema={mergedUiSchema}
                                  formData={formData}
                                  onChange={setFormData}
                                  onSubmit={(data) => {
                                      setSubmittedData(data)
                                      console.log('提交的数据：', data)
                                  }}
                              />
                          </Suspense>
                        {submittedData && (
                            <details className="submitted-data">
                              <summary>查看提交的数据</summary>
                              <pre>{JSON.stringify(submittedData, null, 2)}</pre>
                            </details>
                        )}
                      </div>
                  ) : (
                      <JsonPanel
                          value={activeTab === 'STRUCTURE_MODEL'
                              ? preview.structureModel
                              : artifacts[activeTab]}
                      />
                  )}
                  {warnings.length > 0 && (
                      <div className="warnings">
                        <h3>告警</h3>
                        {warnings.map((warning, index) => (
                            <div className="warning" key={`${warning.path}-${index}`}>
                              <strong>{warning.code}</strong>
                              <code>{warning.path}</code>
                              <span>{warning.message}</span>
                            </div>
                        ))}
                      </div>
                  )}
                </>
            )}
          </article>
        </section>

        <section className="lower-grid">
          <article className="panel definitions-panel">
            <div className="panel-heading">
              <div>
                <span className="step">03</span>
                <h2>结构定义</h2>
              </div>
              <button className="text-button" onClick={handleRefresh}>刷新列表</button>
            </div>

            {definitions.length === 0 ? (
                <p className="muted">保存结构定义后会展示版本列表。</p>
            ) : definitions.map((definition) => (
                <div
                    className={selected === definition.id ? 'definition selected' : 'definition'}
                    key={definition.id}
                    onClick={() => setSelected(definition.id)}
                >
                  <div>
                    <strong>{definition.name}</strong>
                    <span>
                      {definition.description || '暂无描述'} / {definition.status || 'SAVED'}
                      {definition.draft ? ' / 草稿中' : ''}
                    </span>
                  </div>
                  <div className="version-list">
                    {definition.versions.map((version) => (
                        <div className="version-actions" key={version.versionNo}>
                          <button
                              className={definition.activeVersionNo === version.versionNo
                                  ? 'version active-version'
                                  : 'version'}
                              onClick={(event) => {
                                event.stopPropagation()
                                setSelected(definition.id)
                                handleActivate(version.versionNo)
                              }}
                          >
                            v{version.versionNo}
                            <small>{version.validationMode}</small>
                          </button>
                          <button
                              className="text-button"
                              onClick={(event) => {
                                event.stopPropagation()
                                setSelected(definition.id)
                                handleCopyVersionToDraft(version.versionNo)
                              }}
                          >
                            复制到草稿
                          </button>
                          {definition.activeVersionNo && definition.activeVersionNo !== version.versionNo && (
                              <button
                                  className="text-button"
                                  onClick={(event) => {
                                    event.stopPropagation()
                                    setSelected(definition.id)
                                    handleDiff({ fromVersionNo: definition.activeVersionNo, toVersionNo: version.versionNo })
                                  }}
                              >
                                Diff
                              </button>
                          )}
                        </div>
                    ))}
                    {definition.draft && definition.activeVersionNo && (
                        <button
                            className="text-button"
                            onClick={(event) => {
                              event.stopPropagation()
                              setSelected(definition.id)
                              handleDiff({ fromVersionNo: definition.activeVersionNo, toDraft: true })
                            }}
                        >
                          比较草稿
                        </button>
                    )}
                  </div>
                </div>
            ))}
            {diffResult && (
                <div className="validation-result">
                  <strong>结构 Diff：{diffResult.changes.length} 处变化</strong>
                  <span>
                    v{diffResult.fromVersionNo || '当前'} → {diffResult.toDraft ? '草稿' : `v${diffResult.toVersionNo || '当前'}`}
                  </span>
                  {diffResult.changes.slice(0, 12).map((item, index) => (
                      <p key={`${item.category}-${item.path}-${index}`}>
                        <code>{item.category}</code> {item.changeType} <code>{item.path || '$'}</code>
                        {item.before !== null && item.before !== undefined ? ` 之前：${item.before}` : ''}
                        {item.after !== null && item.after !== undefined ? ` 之后：${item.after}` : ''}
                      </p>
                  ))}
                </div>
            )}
          </article>

          <article className="panel validation-panel">
            <div className="panel-heading">
              <div>
                <span className="step">04</span>
                <h2>数据校验</h2>
              </div>
              <span className="status-pill">
              {selectedDefinition ? `v${selectedDefinition.activeVersionNo || '未上线'}` : '未选择'}
            </span>
            </div>

            <textarea
                aria-label="待校验 JSON"
                className="code-editor validation-editor"
                value={payloadText}
                onChange={(event) => setPayloadText(event.target.value)}
                spellCheck="false"
            />
            <button className="button primary" onClick={handleValidate} disabled={Boolean(busy)}>
              校验数据
            </button>

            {validation && (
                <div className={validation.valid ? 'validation-result valid' : 'validation-result invalid'}>
                  <strong>{validation.valid ? '校验通过' : `发现 ${validation.errors.length} 个问题`}</strong>
                  <span>版本 v{validation.versionNo} / {validation.mode}</span>
                  {validation.errors.map((item, index) => (
                      <p key={`${item.path}-${index}`}><code>{item.path}</code> {item.message}</p>
                  ))}
                </div>
            )}
          </article>
        </section>
                </>
          )}
          {activeModuleKey !== 'home-dashboard'
              && activeModuleKey !== 'biz-pay-cashier'
              && activeModuleKey !== 'flow'
              && activeModuleKey !== 'tool-gen'
              && activeModuleKey !== 'monitor-server'
              && activeModuleKey !== 'structure' && (
                <section className="module-dashboard hb-page-card">
                  <header className="module-page-header">
                    <div>
                      <p className="page-breadcrumb">{activeAdminModule.category} / {activeAdminModule.name}</p>
                      <h1>{activeAdminModule.name}</h1>
                      <p>{activeAdminModule.description}</p>
                    </div>
                    <div className="module-page-meta">
                      <span className="status-pill">{activeAdminModule.status}</span>
                      <code>{activeAdminModule.permissionPrefix}</code>
                    </div>
                  </header>

                  <div className="module-toolbar panel">
                    {activeModuleKey === 'tool-job' && (
                        <>
                          <button className="button ghost" disabled={Boolean(busy)} onClick={() => handleJobAction('run')}>执行</button>
                          <button className="button ghost" disabled={Boolean(busy)} onClick={() => handleJobAction('pause')}>暂停</button>
                          <button className="button ghost" disabled={Boolean(busy)} onClick={() => handleJobAction('resume')}>恢复</button>
                          <button className="button ghost" disabled={Boolean(busy)} onClick={() => handleJobAction('refresh')}>刷新调度</button>
                        </>
                    )}
                    {(activeAdminModule.actions || []).map((action) => (
                        <button
                            className={action.includes('新增') ? 'button primary' : 'button ghost'}
                            key={action}
                            onClick={() => handleModuleAction(action)}
                            disabled={Boolean(busy) || (!activeResource && action !== '刷新')}
                        >
                          {action}
                        </button>
                    ))}
                  </div>

                  {!activeResource ? (
                      <div className="panel module-placeholder">
                        <h2>专用工作台</h2>
                        <p>{activeAdminModule.name} 暂未绑定通用资源接口，将通过专用页面或 API 接入。</p>
                      </div>
                  ) : (
                  <>
                  <div className="module-metrics">
                    {(activeAdminModule.metrics || []).map((metric) => (
                        <div className="metric-tile glass-section" key={metric.label}>
                          <span>{metric.label}</span>
                          <strong>{metric.value}</strong>
                          <small>{metric.hint}</small>
                        </div>
                    ))}
                  </div>

                  <div className="panel module-table-panel">
                    <div className="panel-heading">
                      <div>
                        <span className="step">列表</span>
                        <h2>{activeAdminModule.name}列表</h2>
                      </div>
                      <span className="status-pill">{activeResource ? (resourceEditable ? '可维护' : '只读') : '未接入'}</span>
                    </div>
                    <ResourceTable
                        key={activeResource}
                        moduleName={activeAdminModule.name}
                        columns={activeColumns}
                        records={activeRecords}
                        selectedRow={selectedResourceRow}
                        editable={resourceEditable}
                        onSelect={setSelectedResourceRow}
                        onEdit={(record) => {
                          setSelectedResourceRow(record)
                          openResourceDialog('edit', record)
                        }}
                        onDelete={deleteResourceRow}
                    />
                  </div>
                  </>
                  )}
                </section>
          )}
        </AdminLayout>
        <ResourceDialog
            open={resourceDialog.open}
            mode={resourceDialog.mode}
            resource={activeResource}
            values={resourceFormValues}
            busy={busy === `resource-${resourceDialog.mode}`}
            onClose={closeResourceDialog}
            onSubmit={submitResourceDialog}
        />
        <RoleMenuDialog
            open={roleMenuDialog.open}
            role={roleMenuDialog.role}
            busy={busy === 'role-menu-save'}
            onClose={closeRoleMenuDialog}
            onSubmit={submitRoleMenuDialog}
        />
      </>
  )
}
