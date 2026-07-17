const SECTION_CATALOG = [
    {
        id: 'workspace',
        menuCode: 'catalog:workspace',
        name: '工作台',
        order: 10,
        groups: []
    },
    {
        id: 'platform-governance',
        menuCode: 'catalog:platform-governance',
        name: '平台治理',
        order: 20,
        groups: [
            {id: 'platform-accounts', menuCode: 'catalog:platform-accounts', name: '账号与组织', order: 10},
            {id: 'platform-permissions', menuCode: 'catalog:platform-permissions', name: '权限配置', order: 20},
            {id: 'platform-configuration', menuCode: 'catalog:platform-configuration', name: '平台配置', order: 30},
            {id: 'platform-access', menuCode: 'catalog:platform-access', name: '接入安全', order: 40},
            {id: 'platform-audit', menuCode: 'catalog:platform-audit', name: '审计与会话', order: 50},
            {id: 'platform-extensions', menuCode: 'catalog:platform-extensions', name: '扩展功能', order: 90}
        ]
    },
    {
        id: 'data-automation',
        menuCode: 'catalog:data-automation',
        name: '数据与自动化',
        order: 30,
        groups: [
            {id: 'data-modeling', menuCode: 'catalog:data-modeling', name: '数据模型', order: 10},
            {id: 'flow-automation', menuCode: 'catalog:flow-automation', name: '流程自动化', order: 20},
            {id: 'approval-center', menuCode: 'catalog:approval-center', name: '审批中心', order: 30},
            {id: 'automation-extensions', menuCode: 'catalog:automation-extensions', name: '扩展能力', order: 90}
        ]
    },
    {
        id: 'business-center',
        menuCode: 'catalog:business-center',
        name: '业务中心',
        order: 40,
        groups: [
            {id: 'payment-center', menuCode: 'catalog:payment-center', name: '支付中心', order: 10},
            {id: 'official-account', menuCode: 'catalog:official-account', name: '公众号', order: 20},
            {id: 'report-center', menuCode: 'catalog:report-center', name: '报表中心', order: 30},
            {id: 'mobile-apps', menuCode: 'catalog:mobile-apps', name: '移动应用', order: 40},
            {id: 'business-extensions', menuCode: 'catalog:business-extensions', name: '扩展业务', order: 90}
        ]
    },
    {
        id: 'operations',
        menuCode: 'catalog:operations',
        name: '运行保障',
        order: 50,
        groups: [
            {id: 'runtime-monitoring', menuCode: 'catalog:runtime-monitoring', name: '运行监控', order: 10},
            {id: 'operations-extensions', menuCode: 'catalog:operations-extensions', name: '扩展监控', order: 90}
        ]
    },
    {
        id: 'developer-tools',
        menuCode: 'catalog:developer-tools',
        name: '开发工具',
        order: 60,
        groups: []
    }
]

const ROUTE_REGISTRY = {
    dashboard: routeConfig('home-dashboard', '运营总览', 'workspace', null, 10, '/dashboard', 'dashboard:view'),

    'system:tenant': routeConfig('system-tenant', '租户管理', 'platform-governance', 'platform-accounts', 10, '/system/tenant'),
    'system:user': routeConfig('system-user', '用户管理', 'platform-governance', 'platform-accounts', 20, '/system/user'),
    'system:dept': routeConfig('system-dept', '部门管理', 'platform-governance', 'platform-accounts', 30, '/system/dept'),
    'system:post': routeConfig('system-post', '岗位管理', 'platform-governance', 'platform-accounts', 40, '/system/post'),
    'system:role': routeConfig('system-role', '角色管理', 'platform-governance', 'platform-permissions', 10, '/system/role'),
    'system:menu': routeConfig('system-menu', '菜单管理', 'platform-governance', 'platform-permissions', 20, '/system/menu'),
    'system:dict': routeConfig('system-dict', '字典管理', 'platform-governance', 'platform-configuration', 10, '/system/dict'),
    'system:dict:type': routeConfig('system-dict-type', '字典类型', 'platform-governance', 'platform-configuration', 10, '/system/dict/types'),
    'system:dict:data': routeConfig('system-dict-data', '字典数据', 'platform-governance', 'platform-configuration', 11, '/system/dict/data'),
    'system:config': routeConfig('system-config', '参数配置', 'platform-governance', 'platform-configuration', 20, '/system/config'),
    'system:notice': routeConfig('system-notice', '通知公告', 'platform-governance', 'platform-configuration', 30, '/system/notice'),
    'system:oauth': routeConfig('system-oauth', 'OAuth 客户端', 'platform-governance', 'platform-access', 10, '/system/oauth'),
    'system:social': routeConfig('system-social', '社交登录', 'platform-governance', 'platform-access', 20, '/system/social'),
    'monitor:operlog': routeConfig('monitor-operlog', '操作日志', 'platform-governance', 'platform-audit', 10, '/system/audit/operations'),
    'monitor:loginlog': routeConfig('monitor-loginlog', '登录日志', 'platform-governance', 'platform-audit', 20, '/system/audit/logins'),
    'monitor:online': routeConfig('monitor-online', '在线会话', 'platform-governance', 'platform-audit', 30, '/system/sessions'),

    'structure:definition': routeConfig('structure', '结构定义', 'data-automation', 'data-modeling', 10, '/structure-definitions'),
    'flow:studio': routeConfig('flow', '流程设计器', 'data-automation', 'flow-automation', 10, '/flows/studio', 'flow:studio:list'),
    'flow:definition': routeConfig('flow-definition', '流程定义', 'data-automation', 'flow-automation', 20, '/flows/definitions', 'flow:studio:list'),
    'flow:component': routeConfig('flow-component', '节点组件', 'data-automation', 'flow-automation', 30, '/flows/components', 'flow:studio:list'),
    'flow:credential': routeConfig('flow-credential', '连接凭据', 'data-automation', 'flow-automation', 40, '/flows/credentials', 'flow:studio:list'),
    'flow:run': routeConfig('flow-run', '运行记录', 'data-automation', 'flow-automation', 50, '/flows/runs', 'flow:studio:list'),
    'biz:workflow': routeConfig('biz-workflow', '审批工作台', 'data-automation', 'approval-center', 10, '/workflow'),
    'biz:workflow:definition': routeConfig('biz-workflow-definition', '审批定义', 'data-automation', 'approval-center', 20, '/workflow/definitions'),
    'biz:workflow:instance': routeConfig('biz-workflow-instance', '流程实例', 'data-automation', 'approval-center', 30, '/workflow/instances'),
    'biz:workflow:todo': routeConfig('biz-workflow-todo', '待办任务', 'data-automation', 'approval-center', 40, '/workflow/tasks', 'biz:workflow:todo'),

    'biz:pay': routeConfig('biz-pay', '支付工作台', 'business-center', 'payment-center', 10, '/pay'),
    'biz:pay:channel': routeConfig('biz-pay-channel', '支付渠道', 'business-center', 'payment-center', 20, '/pay/channels'),
    'biz:pay:order': routeConfig('biz-pay-order', '支付订单', 'business-center', 'payment-center', 30, '/pay/orders'),
    'biz:pay:notify': routeConfig('biz-pay-notify', '通知日志', 'business-center', 'payment-center', 40, '/pay/notify-logs'),
    'biz:pay:cashier': routeConfig('biz-pay-cashier', '收银台', 'business-center', 'payment-center', 50, '/pay/cashier'),
    'biz:mp': routeConfig('biz-mp', '公众号管理', 'business-center', 'official-account', 10, '/mp'),
    'biz:mp:account': routeConfig('biz-mp-account', '公众号账号', 'business-center', 'official-account', 20, '/mp/accounts'),
    'biz:mp:menu': routeConfig('biz-mp-menu', '自定义菜单', 'business-center', 'official-account', 30, '/mp/menus'),
    'biz:mp:material': routeConfig('biz-mp-material', '素材管理', 'business-center', 'official-account', 40, '/mp/materials'),
    'biz:mp:reply': routeConfig('biz-mp-reply', '自动回复', 'business-center', 'official-account', 50, '/mp/replies'),
    'biz:report': routeConfig('biz-report', '报表工作台', 'business-center', 'report-center', 10, '/report'),
    'biz:report:dataset': routeConfig('biz-report-dataset', '数据集', 'business-center', 'report-center', 20, '/report/datasets'),
    'biz:report:template': routeConfig('biz-report-template', '报表模板', 'business-center', 'report-center', 30, '/report/templates'),
    'biz:mobile': routeConfig('biz-mobile', '应用搭建', 'business-center', 'mobile-apps', 10, '/mobile'),
    'biz:mobile:app': routeConfig('biz-mobile-app', '应用管理', 'business-center', 'mobile-apps', 20, '/mobile/apps'),
    'biz:mobile:page': routeConfig('biz-mobile-page', '页面管理', 'business-center', 'mobile-apps', 30, '/mobile/pages'),
    'biz:mobile:route': routeConfig('biz-mobile-route', 'API 路由', 'business-center', 'mobile-apps', 40, '/mobile/api-routes'),

    'monitor:server': routeConfig('monitor-server', '服务器', 'operations', 'runtime-monitoring', 10, '/monitor/server'),
    'monitor:cache': routeConfig('monitor-cache', '缓存监控', 'operations', 'runtime-monitoring', 20, '/monitor/cache'),
    'monitor:druid': routeConfig('monitor-druid', '数据源监控', 'operations', 'runtime-monitoring', 30, '/monitor/druid'),

    'tool:job': routeConfig('tool-job', '调度任务', 'developer-tools', null, 10, '/tool/jobs'),
    'tool:gen': routeConfig('tool-gen', '代码生成', 'developer-tools', null, 20, '/tool/gen')
}

const COMPOSITE_ROUTE_CHILDREN = {
    'system:dict': ['system:dict:type', 'system:dict:data'],
    'biz:workflow': ['biz:workflow:definition', 'biz:workflow:instance', 'biz:workflow:todo'],
    'biz:pay': ['biz:pay:channel', 'biz:pay:order', 'biz:pay:notify', 'biz:pay:cashier'],
    'biz:mp': ['biz:mp:account', 'biz:mp:menu', 'biz:mp:material', 'biz:mp:reply'],
    'biz:report': ['biz:report:dataset', 'biz:report:template'],
    'biz:mobile': ['biz:mobile:app', 'biz:mobile:page', 'biz:mobile:route']
}

const AUGMENTED_ROUTE_CHILDREN = {
    'flow:definition': ['flow:credential', 'flow:run']
}

const COMPOSITE_CHILD_VIEW_PERMISSIONS = {
    'biz:workflow:todo': 'biz:workflow:todo',
    'biz:pay:cashier': 'biz:pay:order'
}

const FRONT_ID_TO_MENU_CODE = Object.entries(ROUTE_REGISTRY).reduce((result, [menuCode, config]) => {
    result[config.id] = menuCode
    return result
}, {})

const BACKEND_CATALOG_SECTIONS = {
    'root:system': 'platform-governance',
    'root:flow': 'data-automation',
    'root:business': 'business-center',
    'root:monitor': 'operations',
    'root:tool': 'developer-tools'
}

function routeConfig(id, name, sectionId, groupId, order, path, permissionPrefix) {
    return {id, name, sectionId, groupId, order, path, permissionPrefix}
}

function unwrapFields(value) {
    if (!value || typeof value !== 'object' || Array.isArray(value)) return value
    if (!value.fields || typeof value.fields !== 'object' || Array.isArray(value.fields)) return value
    const {fields, ...envelope} = value
    return {...envelope, ...fields}
}

function routeList(value) {
    if (Array.isArray(value)) return value
    const unwrapped = unwrapFields(value)
    if (!unwrapped || typeof unwrapped !== 'object') return []
    if (Array.isArray(unwrapped.routes)) return unwrapped.routes
    if (Array.isArray(unwrapped.items)) return unwrapped.items
    if (Array.isArray(unwrapped.children) && !menuCodeFromRoute(unwrapped)) return unwrapped.children
    return [unwrapped]
}

function menuCodeFromRoute(route) {
    const direct = route?.menuCode || route?.menu_code || route?.code
    if (direct) return String(direct).trim()

    const legacyId = route?.id == null ? '' : String(route.id).trim()
    if (ROUTE_REGISTRY[legacyId]) return legacyId
    if (FRONT_ID_TO_MENU_CODE[legacyId]) return FRONT_ID_TO_MENU_CODE[legacyId]
    if (legacyId === 'dashboard' || legacyId.includes(':')) return legacyId
    return ''
}

function normalizedType(route, menuCode) {
    const type = String(route?.type || route?.menuType || 'MENU').trim().toUpperCase()
    if (type === 'CATALOG' || type === 'DIR' || menuCode.startsWith('root:')) return 'DIR'
    return type || 'MENU'
}

function isVisible(route) {
    const visible = route?.visible
    if (visible === false || visible === 0) return false
    if (typeof visible === 'string' && ['0', 'false', 'hidden'].includes(visible.trim().toLowerCase())) return false

    const hidden = route?.hidden
    return hidden !== true && hidden !== 1 && String(hidden || '').trim().toLowerCase() !== 'true'
}

function isEnabled(route) {
    const disabled = route?.disabled
    if (disabled === true || disabled === 1) return false
    if (typeof disabled === 'string' && ['1', 'true'].includes(disabled.trim().toLowerCase())) return false

    const status = String(route?.status || 'ENABLED').trim().toUpperCase()
    return status !== 'DISABLED' && status !== 'INACTIVE'
}

function stableIdForMenuCode(menuCode) {
    const registryId = ROUTE_REGISTRY[menuCode]?.id
    if (registryId) return registryId

    const normalized = menuCode
        .trim()
        .toLowerCase()
        .replace(/[:/_.\s]+/g, '-')
        .replace(/[^a-z0-9\u4e00-\u9fff-]+/g, '-')
        .replace(/-{2,}/g, '-')
        .replace(/^-|-$/g, '')
    return normalized || `menu-${stringHash(menuCode)}`
}

function stringHash(value) {
    let hash = 0
    for (const character of String(value)) {
        hash = ((hash * 31) + character.codePointAt(0)) >>> 0
    }
    return hash.toString(36)
}

function placementFor(menuCode) {
    const registered = ROUTE_REGISTRY[menuCode]
    if (registered) return registered

    if (menuCode.startsWith('system:')) {
        return routeConfig(stableIdForMenuCode(menuCode), null, 'platform-governance', 'platform-extensions', 90)
    }
    if (menuCode.startsWith('structure:')) {
        return routeConfig(stableIdForMenuCode(menuCode), null, 'data-automation', 'data-modeling', 90)
    }
    if (menuCode.startsWith('flow:')) {
        return routeConfig(stableIdForMenuCode(menuCode), null, 'data-automation', 'automation-extensions', 90)
    }
    if (menuCode.startsWith('biz:workflow')) {
        return routeConfig(stableIdForMenuCode(menuCode), null, 'data-automation', 'approval-center', 90)
    }
    if (menuCode.startsWith('biz:')) {
        return routeConfig(stableIdForMenuCode(menuCode), null, 'business-center', 'business-extensions', 90)
    }
    if (menuCode.startsWith('monitor:')) {
        return routeConfig(stableIdForMenuCode(menuCode), null, 'operations', 'operations-extensions', 90)
    }
    if (menuCode.startsWith('tool:')) {
        return routeConfig(stableIdForMenuCode(menuCode), null, 'developer-tools', null, 90)
    }
    return routeConfig(stableIdForMenuCode(menuCode), null, 'business-center', 'business-extensions', 99)
}

function permissionPrefixFor(menuCode, config) {
    if (config.permissionPrefix) return config.permissionPrefix
    return `${menuCode}:list`
}

function normalizeLeaf(route, menuCode, sequence) {
    const config = placementFor(menuCode)
    const backendId = route.backendId ?? route.id ?? null
    const originalType = String(route.type || route.menuType || 'MENU').trim().toUpperCase()
    const name = config.name || route.menuName || route.name || menuCode
    const path = config.path || route.routePath || route.path || ''
    const permissionPrefix = permissionPrefixFor(menuCode, config)
    const backendSortNo = Number(route.sortNo)
    const sortNo = Number.isFinite(config.order)
        ? config.order
        : (Number.isFinite(backendSortNo) ? backendSortNo : sequence)

    return {
        sectionId: config.sectionId,
        groupId: config.groupId,
        sortNo,
        sequence,
        route: {
            ...route,
            id: config.id || stableIdForMenuCode(menuCode),
            key: config.id || stableIdForMenuCode(menuCode),
            backendId,
            backendParentId: route.backendParentId ?? route.parentId ?? null,
            backendType: route.backendType || originalType,
            menuCode,
            name,
            menuName: name,
            type: 'MENU',
            menuType: 'MENU',
            path,
            routePath: path,
            permission: permissionPrefix,
            permissionPrefix,
            visible: true,
            status: route.status || 'ENABLED',
            sortNo,
            children: []
        }
    }
}

function normalizeAuthorizedLeaves(route, menuCode, state, permissionCodes, declaredMenuCodes) {
    const compositeChildren = COMPOSITE_ROUTE_CHILDREN[menuCode]
    const augmentedChildren = AUGMENTED_ROUTE_CHILDREN[menuCode]
    const childMenuCodes = compositeChildren || (augmentedChildren ? [menuCode, ...augmentedChildren] : null)
    if (!childMenuCodes) {
        const leaf = normalizeLeaf(route, menuCode, state.sequence)
        state.sequence += 1
        return [leaf]
    }

    const parentPermission = permissionPrefixFor(menuCode, placementFor(menuCode))
    return childMenuCodes.filter((childMenuCode) => {
        if (childMenuCode !== menuCode && declaredMenuCodes.has(childMenuCode)) return false
        const required = COMPOSITE_CHILD_VIEW_PERMISSIONS[childMenuCode] || parentPermission
        return permissionCodes.has('*')
            || permissionCodes.has('*:*')
            || permissionCodes.has(required)
    }).map((childMenuCode) => {
        const leaf = normalizeLeaf(route, childMenuCode, state.sequence)
        state.sequence += 1
        leaf.route = {
            ...leaf.route,
            sourceMenuCode: menuCode,
            permission: parentPermission,
            permissionPrefix: parentPermission
        }
        return leaf
    })
}

function collectAuthorizedLeaves(value, target, catalogs, seenMenuCodes, state, permissionCodes) {
    for (const candidate of routeList(value)) {
        const route = unwrapFields(candidate)
        if (!route || typeof route !== 'object' || Array.isArray(route)) continue
        if (!isVisible(route) || !isEnabled(route)) continue

        const menuCode = menuCodeFromRoute(route)
        const type = normalizedType(route, menuCode)
        const sectionId = BACKEND_CATALOG_SECTIONS[menuCode]
        if (type === 'DIR' && sectionId && !catalogs.has(sectionId)) {
            catalogs.set(sectionId, route)
        }
        const requiredPermission = menuCode ? permissionPrefixFor(menuCode, placementFor(menuCode)) : ''
        const permissionAllowed = permissionCodes.has('*')
            || permissionCodes.has('*:*')
            || permissionCodes.has(requiredPermission)
        if (type !== 'BUTTON' && type !== 'DIR' && menuCode && permissionAllowed && !seenMenuCodes.has(menuCode)) {
            seenMenuCodes.add(menuCode)
            const leaves = normalizeAuthorizedLeaves(route, menuCode, state, permissionCodes, state.declaredMenuCodes)
            leaves.forEach((leaf) => seenMenuCodes.add(leaf.route.menuCode))
            target.push(...leaves)
        }

        if (type !== 'BUTTON') {
            collectAuthorizedLeaves(route.children || [], target, catalogs, seenMenuCodes, state, permissionCodes)
        }
    }
}

function collectDeclaredMenuCodes(value, target = new Set()) {
    for (const candidate of routeList(value)) {
        const route = unwrapFields(candidate)
        if (!route || typeof route !== 'object' || Array.isArray(route)) continue
        const menuCode = menuCodeFromRoute(route)
        if (menuCode) target.add(menuCode)
        collectDeclaredMenuCodes(route.children || [], target)
    }
    return target
}

function comparePlacement(left, right) {
    return left.sortNo - right.sortNo || left.sequence - right.sequence
}

function directoryNode(definition, children, parentId = null, backendRoute = null) {
    const originalType = String(backendRoute?.type || backendRoute?.menuType || '').trim().toUpperCase()
    const backendPath = backendRoute?.backendPath || backendRoute?.routePath || backendRoute?.path || ''
    return {
        ...(backendRoute || {}),
        id: definition.id,
        key: definition.id,
        backendId: backendRoute?.backendId ?? backendRoute?.id ?? null,
        backendParentId: backendRoute?.backendParentId ?? backendRoute?.parentId ?? null,
        backendType: backendRoute?.backendType || originalType || null,
        backendPath,
        parentId,
        menuCode: backendRoute ? menuCodeFromRoute(backendRoute) : definition.menuCode,
        name: definition.name,
        menuName: definition.name,
        type: 'DIR',
        menuType: 'DIR',
        path: '',
        routePath: '',
        component: null,
        permission: '',
        permissionPrefix: '',
        visible: true,
        status: backendRoute?.status || 'ENABLED',
        sortNo: definition.order,
        children
    }
}

function withParent(route, parentId) {
    return {...route, parentId}
}

function buildSection(section, placements, catalogs) {
    const sectionPlacements = placements.filter((item) => item.sectionId === section.id)
    if (sectionPlacements.length === 0) return null

    const directChildren = sectionPlacements
        .filter((item) => !item.groupId)
        .sort(comparePlacement)
        .map((item) => withParent(item.route, section.id))

    const groupedChildren = section.groups.map((group) => {
        const children = sectionPlacements
            .filter((item) => item.groupId === group.id)
            .sort(comparePlacement)
            .map((item) => withParent(item.route, group.id))
        return children.length > 0 ? directoryNode(group, children, section.id) : null
    }).filter(Boolean)

    return directoryNode(section, [...directChildren, ...groupedChildren], null, catalogs.get(section.id) || null)
}

/**
 * Converts authorized IAM route records into the stable, curated admin navigation tree.
 * Synthetic catalog nodes are emitted only when at least one returned leaf belongs to them.
 */
export function normalizeAdminRoutes(rawRoutes, permissions = []) {
    const placements = []
    const catalogs = new Map()
    const permissionCodes = new Set((permissions || []).map((item) => String(item)))
    const declaredMenuCodes = collectDeclaredMenuCodes(rawRoutes)
    collectAuthorizedLeaves(
        rawRoutes,
        placements,
        catalogs,
        new Set(),
        {sequence: 0, declaredMenuCodes},
        permissionCodes
    )
    return SECTION_CATALOG.map((section) => buildSection(section, placements, catalogs)).filter(Boolean)
}
