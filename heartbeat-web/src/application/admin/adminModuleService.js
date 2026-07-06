import {
  actionsForResource,
  getResourceDefinition,
  isResourceReadOnly,
  payloadDisplay
} from '../../domain/admin/resourceDefinitions'
import {
  appPathForMenu,
  filterNavigableTree,
  findMenuById,
  firstAvailableMenu,
  isNavigableMenu,
  resolveTopModuleIdByPath
} from '../../domain/admin/navigationPolicy'

export function categoryFromPermission(permission = '') {
  if (permission.startsWith('system:')) return '系统管理'
  if (permission.startsWith('monitor:')) return '系统监控'
  if (permission.startsWith('tool:')) return '系统工具'
  if (permission.startsWith('structure:')) return '数据配置'
  return '平台菜单'
}

function moduleFromRoute(route) {
  const permission = route.permission || ''
  const resource = resourceFromModule({ key: route.id, permissionPrefix: permission })
  const definition = getResourceDefinition(resource || 'default')
  return {
    key: route.id,
    name: route.name,
      appPath: appPathForMenu(route),
    category: categoryFromPermission(permission),
    description: `${route.name}对接后端 ${resource ? `/api/v1/admin/resources/${resource}` : '专用接口'}，数据来自真实持久化层。`,
    permissionPrefix: permission,
    status: route.visible === false ? '隐藏菜单' : '已接入',
    resource,
    actions: actionsForResource(resource),
    metrics: [
      { label: '路由', value: route.path || '—', hint: route.component || '未绑定组件' },
      { label: '权限', value: permission || '—', hint: 'sys_menu.permission_code' },
      { label: '资源', value: resource || 'structure', hint: resource ? 'admin resources' : '独立工作台' }
    ],
    columns: definition.columns,
    records: []
  }
}

export function flattenRouteModules(routes = [], target = []) {
  routes.forEach((route) => {
      if (isNavigableMenu(route)) {
      target.push(moduleFromRoute(route))
    }
    flattenRouteModules(route.children || [], target)
  })
  return target
}

export function splitTopSideMenus(routeTree = []) {
    return filterNavigableTree(routeTree).filter((route) => route.type === 'DIR')
}

export function sideMenusForTop(routeTree = [], topModuleId) {
    const top = filterNavigableTree(routeTree).find((item) => item.id === topModuleId)
  return top?.children || []
}

export function resolveTopModuleId(routeTree = [], menuId) {
  if (!menuId || routeTree.length === 0) return routeTree[0]?.id
  for (const top of routeTree) {
    if (top.id === menuId) return top.id
    if (containsMenuId(top.children || [], menuId)) return top.id
  }
  return routeTree[0]?.id
}

export function resolveTopModuleIdForPath(routeTree = [], pathname) {
    return resolveTopModuleIdByPath(routeTree, pathname)
}

export function firstMenuInTree(nodes = []) {
    return firstAvailableMenu(nodes)
}

function containsMenuId(nodes, menuId) {
    return Boolean(findMenuById(nodes, menuId))
}

export function resourceFromModule(module) {
  const permission = module?.permissionPrefix || ''
  if (module?.key === 'system-menu' || permission.startsWith('system:menu')) return 'menus'
  if (permission.startsWith('system:user')) return 'users'
  if (permission.startsWith('system:dept')) return 'depts'
  if (permission.startsWith('system:post')) return 'posts'
  if (permission.startsWith('system:role')) return 'roles'
  if (permission.startsWith('system:dict')) return 'dict-types'
  if (permission.startsWith('system:config')) return 'configs'
  if (permission.startsWith('system:notice')) return 'notices'
  if (permission.startsWith('monitor:operlog')) return 'oper-logs'
  if (permission.startsWith('monitor:loginlog')) return 'login-logs'
  if (permission.startsWith('monitor:online')) return 'online-sessions'
  if (permission.startsWith('monitor:server')) return null
  if (permission.startsWith('tool:job')) return 'jobs'
  return null
}

export function columnsForResource(resource, module) {
  if (resource) return getResourceDefinition(resource).columns
  if (module?.columns?.length) return module.columns
  return getResourceDefinition('default').columns
}

export function recordFromResource(resource, item) {
  const base = { id: item.id, raw: item }

  if (resource === 'menus') {
    return {
      ...base,
      名称: item.name,
      类型: item.type || 'MENU',
      路径: item.path || '—',
      权限标识: item.permission || '—',
      状态: item.status || 'ACTIVE'
    }
  }
  if (resource === 'users') {
    return {
      ...base,
      用户名: item.username,
      昵称: item.nickname || '—',
      '部门 ID': item.deptId || '—',
      邮箱: item.email || '—',
      状态: item.status || 'ACTIVE'
    }
  }
  if (resource === 'depts') {
    return {
      ...base,
      部门名称: item.name || '—',
      部门编码: item.code || '—',
      '上级 ID': item.parentId || '—',
      状态: item.status || 'ACTIVE'
    }
  }
  if (resource === 'posts') {
    return {
      ...base,
      岗位名称: item.name || '—',
      岗位编码: item.code || '—',
      排序: item.sortNo ?? '—',
      状态: item.status || 'ACTIVE'
    }
  }
  if (resource === 'roles') {
    return {
      ...base,
      角色名称: item.name || '—',
      权限字符: item.code || '—',
      数据范围: item.dataScope || '—',
      状态: item.status || 'ACTIVE'
    }
  }
  if (resource === 'dict-types') {
    return {
      ...base,
      字典名称: item.name || '—',
      字典类型: item.code || '—',
      备注: item.description || '—',
      状态: item.status || 'ACTIVE'
    }
  }
  if (resource === 'configs') {
    return {
      ...base,
      参数名称: item.name || '—',
      参数键名: item.code || '—',
      参数键值: payloadDisplay(item.payload),
      状态: item.status || 'ACTIVE'
    }
  }
  if (resource === 'notices') {
    return {
      ...base,
      公告标题: item.name || '—',
      公告类型: item.code || '—',
      状态: item.status || 'ACTIVE',
      备注: item.description || '—'
    }
  }
  if (resource === 'jobs') {
    return {
      ...base,
      任务名称: item.name || '—',
      任务编码: item.code || '—',
      'Cron 表达式': payloadDisplay(item.payload),
      状态: item.status || 'ACTIVE'
    }
  }
  if (resource === 'oper-logs') {
    return {
      ...base,
      操作人: item.name || '—',
      模块: item.code || '—',
      结果: item.status || '—',
      说明: item.description || '—'
    }
  }
  if (resource === 'login-logs') {
    return {
      ...base,
      用户名: item.name || '—',
      账号: item.code || '—',
      结果: item.status || '—',
      说明: item.description || '—'
    }
  }
  if (resource === 'online-sessions') {
    return {
      ...base,
      用户: item.name || '—',
      会话标识: item.code || '—',
      状态: item.status || '—',
      说明: item.description || '—'
    }
  }

  return {
    ...base,
    名称: item.name || '—',
    编码: item.code || '—',
    状态: item.status || 'ACTIVE',
    说明: item.description || '—'
  }
}

export function flattenMenuRows(items = [], target = []) {
  items.forEach((item) => {
    target.push(recordFromResource('menus', item))
    flattenMenuRows(item.children || [], target)
  })
  return target
}

export function toResourceFormValues(resource, row) {
  const definition = getResourceDefinition(resource)
  const source = row?.raw || {}
  const values = {
    ...definition.emptyValues,
    ...source,
    type: source.type || source.menuType || definition.emptyValues.type,
    visible: source.visible === undefined ? definition.emptyValues.visible : String(source.visible)
  }

  if (resource === 'configs' || resource === 'jobs') {
    values.payload = payloadDisplay(source.payload)
    if (values.payload === '—') values.payload = definition.emptyValues.payload || ''
  }

  return values
}

function normalizePayload(resource, value) {
  if (resource === 'configs') {
    if (!value) return '{}'
    try {
      JSON.parse(value)
      return value
    } catch {
      return JSON.stringify({ value })
    }
  }
  if (resource === 'jobs') {
    if (!value) return '{}'
    try {
      JSON.parse(value)
      return value
    } catch {
      return JSON.stringify({ cron: value })
    }
  }
  return value || '{}'
}

export function buildResourcePayload(resource, values, mode) {
  const payload = { ...values }

  if (resource === 'menus') {
    payload.visible = values.visible === true || values.visible === 'true'
    payload.sortNo = Number(values.sortNo || 0)
    return payload
  }

  if (resource === 'users') {
    if (mode === 'edit') delete payload.password
    return payload
  }

  if (resource === 'configs' || resource === 'jobs') {
    payload.payload = normalizePayload(resource, values.payload)
    payload.sortNo = Number(values.sortNo || 0)
    return payload
  }

  payload.sortNo = Number(values.sortNo || 0)
  return payload
}

export { isResourceReadOnly, actionsForResource }
