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
    if (permission.startsWith('dashboard:')) return '工作台'
    if (permission.startsWith('system:')) return '平台治理'
    if (permission.startsWith('structure:') || permission.startsWith('flow:')) return '数据与自动化'
    if (permission.startsWith('biz:workflow')) return '审批中心'
    if (permission.startsWith('biz:')) return '业务中心'
    if (permission.startsWith('monitor:')) return '运行保障'
    if (permission.startsWith('tool:')) return '开发工具'
    return '平台功能'
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
    const key = module?.key || ''
    const identity = `${key} ${permission}`
    if (key === 'system-menu' || identity.includes('system:menu')) return 'menus'
    if (identity.includes('system:tenant')) return 'tenants'
    if (identity.includes('system:user')) return 'users'
    if (identity.includes('system:dept')) return 'depts'
    if (identity.includes('system:post')) return 'posts'
    if (identity.includes('system:role')) return 'roles'
    if (key === 'system-dict-data' || identity.includes('system:dict-data')) return 'dict-data'
    if (key === 'system-dict-type') return 'dict-types'
    if (identity.includes('system:dict')) return 'dict-types'
    if (identity.includes('system:config')) return 'configs'
    if (identity.includes('system:notice')) return 'notices'
    if (identity.includes('system:oauth')) return 'oauth-clients'
    if (identity.includes('system:social')) return 'social-providers'
    if (identity.includes('monitor:operlog')) return 'oper-logs'
    if (identity.includes('monitor:loginlog')) return 'login-logs'
    if (identity.includes('monitor:online')) return 'online-sessions'
    if (identity.includes('monitor:server')) return null
    if (identity.includes('tool:job')) return null
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
        菜单编码: item.menuCode || '—',
        名称: item.menuName || item.name || '—',
        类型: item.menuType || item.type || 'MENU',
        路径: item.routePath || item.path || '—',
        权限模式: item.permissionMode || 'RELATION',
        状态: item.status || 'ENABLED'
    }
  }
  if (resource === 'users') {
    return {
      ...base,
      用户名: item.username,
      昵称: item.nickname || '—',
      '部门 ID': item.deptId || '—',
      邮箱: item.email || '—',
        状态: item.status || 'ENABLED'
    }
  }
  if (resource === 'depts') {
    return {
      ...base,
        部门名称: item.deptName || item.name || '—',
        部门编码: item.deptCode || item.code || '—',
      '上级 ID': item.parentId || '—',
        '负责人 ID': item.leaderUserId || '—',
        状态: item.status || 'ENABLED'
    }
  }
  if (resource === 'posts') {
    return {
      ...base,
        岗位名称: item.postName || '—',
        岗位编码: item.postCode || '—',
      排序: item.sortNo ?? '—',
        状态: item.status || 'ENABLED'
    }
  }
  if (resource === 'roles') {
    return {
      ...base,
      角色名称: item.name || '—',
      权限字符: item.code || '—',
      数据范围: item.dataScope || '—',
        状态: item.status || 'ENABLED'
    }
  }
  if (resource === 'dict-types') {
    return {
      ...base,
        字典名称: item.dictName || '—',
        字典类型: item.dictCode || '—',
      备注: item.description || '—',
        状态: item.status || 'ENABLED'
    }
  }
    if (resource === 'dict-data') {
        return {
            ...base,
            '字典类型 ID': item.dictTypeId ?? '—',
            标签: item.itemLabel || '—',
            键值: item.itemValue ?? '—',
            排序: item.sortNo ?? '—',
            状态: item.status || 'ENABLED'
    }
  }
  if (resource === 'configs') {
    return {
      ...base,
        参数名称: item.configName || '—',
        参数键名: item.configKey || '—',
        参数键值: item.encrypted ? '已加密' : (item.configValue ?? '—'),
        类型: item.valueType || 'STRING',
        状态: item.status || 'ENABLED'
    }
  }
  if (resource === 'notices') {
    return {
      ...base,
        公告标题: item.noticeTitle || '—',
        公告类型: item.noticeType || '—',
        发布状态: item.publishStatus || '—',
        发布时间: item.publishedAt || '—',
        公告内容: item.noticeContent || '—'
    }
  }
  if (resource === 'jobs') {
    return {
      ...base,
        任务名称: item.jobName || item.name || '—',
        任务编码: item.jobCode || item.code || '—',
        'Cron 表达式': item.cronExpression || payloadDisplay(item.payload),
        状态: item.status || 'ENABLED'
    }
  }
    if (resource === 'tenants') {
        return {
            ...base,
            租户编码: item.tenantCode || '—',
            租户名称: item.tenantName || '—',
            租户类型: item.tenantType || '—',
            域名: item.domain || '—',
            联系人: item.contactName || '—',
            状态: item.status || 'ENABLED'
        }
    }
    if (resource === 'oauth-clients') {
        return {
            ...base,
            '客户端 ID': item.clientId || '—',
            客户端名称: item.clientName || '—',
            客户端类型: item.clientType || '—',
            授权范围: item.scopes || '—',
            自动授权: item.autoApprove === undefined ? '—' : (item.autoApprove ? '是' : '否'),
            状态: item.status || 'ENABLED'
        }
    }
    if (resource === 'social-providers') {
        return {
            ...base,
            提供方编码: item.providerCode || '—',
            提供方名称: item.providerName || '—',
            类型: item.providerType || 'OAUTH2',
            '客户端 ID': item.clientId || '—',
            启用: item.enabled === undefined ? '—' : (item.enabled ? '是' : '否'),
            状态: item.status || 'ENABLED'
    }
  }
  if (resource === 'oper-logs') {
    return {
      ...base,
        操作人: item.operatorName || item.operatorId || '—',
        模块: item.moduleCode || '—',
        操作: item.operationName || item.operationType || '—',
        请求路径: item.requestPath || '—',
        结果: item.resultStatus || '—',
        耗时: item.durationMs == null ? '—' : `${item.durationMs} ms`
    }
  }
  if (resource === 'login-logs') {
    return {
      ...base,
        用户名: item.username || '—',
        登录方式: item.loginType || '—',
        '登录 IP': item.loginIp || '—',
        结果: item.resultStatus || '—',
        失败原因: item.failureReason || '—',
        登录时间: item.loggedAt || item.createTime || '—'
    }
  }
  if (resource === 'online-sessions') {
    return {
      ...base,
        '用户 ID': item.userId || '—',
        会话标识: item.sessionId || '—',
        设备: item.deviceName || item.deviceType || '—',
        '登录 IP': item.loginIp || '—',
      状态: item.status || '—',
        最后访问: item.lastAccessAt || '—'
    }
  }

  return {
    ...base,
    名称: item.name || '—',
    编码: item.code || '—',
      状态: item.status || 'ENABLED',
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
      ...source
  }

    if (resource === 'menus') {
        values.menuCode = source.menuCode || definition.emptyValues.menuCode
        values.menuName = source.menuName || source.name || definition.emptyValues.menuName
        values.menuType = source.menuType || source.type || definition.emptyValues.menuType
        values.routePath = source.routePath || source.path || definition.emptyValues.routePath
        values.componentPath = source.componentPath || source.component || definition.emptyValues.componentPath
        values.permissionMode = source.permissionMode || definition.emptyValues.permissionMode
        values.visible = source.visible === undefined ? definition.emptyValues.visible : String(source.visible)
        values.keepAlive = source.keepAlive === undefined ? definition.emptyValues.keepAlive : String(source.keepAlive)
    }

  if (resource === 'configs') {
      values.configName = source.configName || definition.emptyValues.configName
      values.configKey = source.configKey || definition.emptyValues.configKey
      values.configValue = source.configValue ?? definition.emptyValues.configValue
      values.encrypted = source.encrypted === undefined
          ? definition.emptyValues.encrypted
          : String(source.encrypted)
  }

    if (resource === 'social-providers') {
        values.enabled = source.enabled === undefined ? definition.emptyValues.enabled : String(source.enabled)
        values.appSecretCipher = ''
    }

    return values
}

export function buildResourcePayload(resource, values, mode) {
  const payload = { ...values }

  if (resource === 'menus') {
      return {
          parentId: values.parentId,
          menuCode: values.menuCode,
          menuName: values.menuName,
          menuType: values.menuType,
          routePath: values.routePath,
          componentPath: values.componentPath,
          redirectPath: values.redirectPath,
          icon: values.icon,
          visible: values.visible === true || values.visible === 'true',
          keepAlive: values.keepAlive === true || values.keepAlive === 'true',
          externalLink: values.externalLink,
          permissionMode: values.permissionMode || 'RELATION',
          sortNo: Number(values.sortNo || 0),
          status: values.status || 'ENABLED'
      }
  }

  if (resource === 'users') {
    if (mode === 'edit') delete payload.password
    return payload
  }

    if (resource === 'depts') {
        return {
            parentId: values.parentId,
            deptName: values.name,
            deptCode: values.code,
            leaderUserId: values.leaderUserId,
            phone: values.phone,
            email: values.email,
            sortNo: Number(values.sortNo || 0),
            status: values.status || 'ENABLED'
        }
    }

    if (resource === 'configs') {
        return {
            configName: values.configName,
            configKey: values.configKey,
            configValue: values.configValue,
            valueType: values.valueType || 'STRING',
            encrypted: values.encrypted === true || values.encrypted === 'true',
            configGroup: values.configGroup || 'system',
            description: values.description,
            status: values.status || 'ENABLED'
        }
    }

    if (resource === 'social-providers') {
        const socialPayload = {
            providerCode: values.providerCode,
            providerName: values.providerName,
            providerType: values.providerType || 'OAUTH2',
            clientId: values.clientId,
            appKey: values.appKey,
            appSecretCipher: values.appSecretCipher,
            authorizeUrl: values.authorizeUrl,
            tokenUrl: values.tokenUrl,
            userInfoUrl: values.userInfoUrl,
            scopes: values.scopes,
            enabled: values.enabled === true || values.enabled === 'true',
            status: values.status || 'ENABLED'
        }
        if (mode === 'edit' && !String(values.appSecretCipher || '').trim()) {
            delete socialPayload.appSecretCipher
        }
        return socialPayload
  }

  payload.sortNo = Number(values.sortNo || 0)
  return payload
}

export { isResourceReadOnly, actionsForResource }
