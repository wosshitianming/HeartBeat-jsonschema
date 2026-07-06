import {DEFAULT_ADMIN_PATH, matchRoutePattern, normalizePath} from './workspaceRouting'

export const SPECIAL_APP_PATHS = {
    'home-dashboard': '/admin/dashboard',
    structure: DEFAULT_ADMIN_PATH,
    flow: '/admin/flow',
    'tool-gen': '/admin/tool/gen',
    'monitor-server': '/admin/monitor/server',
    'biz-pay-cashier': '/admin/pay/cashier'
}

const MENU_ID_PATTERN = /^\/admin\/module\/([^/?#]+)$/

export function isVisibleMenu(menu = {}) {
    return menu.visible !== false && menu.hidden !== true
}

export function isDisabledMenu(menu = {}) {
    return menu.disabled === true || menu.status === 'DISABLED' || menu.status === 'INACTIVE'
}

export function isNavigableMenu(menu = {}) {
    return Boolean(menu?.id) && menu.type !== 'BUTTON' && isVisibleMenu(menu) && !isDisabledMenu(menu)
}

export function normalizeAppPath(path) {
    if (!path) return ''
    const normalized = normalizePath(path)
    if (normalized.startsWith('/admin')) return normalized
    return normalizePath(`/admin${normalized}`)
}

export function appPathForMenu(menu = {}) {
    if (!menu?.id) return DEFAULT_ADMIN_PATH
    if (SPECIAL_APP_PATHS[menu.id]) return SPECIAL_APP_PATHS[menu.id]
    if (menu.path) return normalizeAppPath(menu.path)
    return `/admin/module/${encodeURIComponent(menu.id)}`
}

export function filterNavigableTree(nodes = []) {
    return nodes
        .filter((node) => node.type !== 'BUTTON' && isVisibleMenu(node) && !isDisabledMenu(node))
        .map((node) => ({
            ...node,
            appPath: appPathForMenu(node),
            children: filterNavigableTree(node.children || [])
        }))
        .filter((node) => node.type === 'DIR' || node.children.length > 0 || isNavigableMenu(node))
}

export function flattenMenus(nodes = [], target = []) {
    nodes.forEach((node) => {
        if (isNavigableMenu(node)) {
            target.push({
                ...node,
                appPath: appPathForMenu(node)
            })
        }
        flattenMenus(node.children || [], target)
    })
    return target
}

export function firstAvailableMenu(nodes = []) {
    for (const node of nodes) {
        if (isNavigableMenu(node)) return {...node, appPath: appPathForMenu(node)}
        const child = firstAvailableMenu(node.children || [])
        if (child) return child
    }
    return null
}

export function findMenuById(nodes = [], menuId) {
    if (!menuId) return null
    for (const node of nodes) {
        if (node.id === menuId) return {...node, appPath: appPathForMenu(node)}
        const child = findMenuById(node.children || [], menuId)
        if (child) return child
    }
    return null
}

export function findMenuByAppPath(nodes = [], pathname) {
    const normalizedPath = normalizeAppPath(pathname)
    const direct = flattenMenus(nodes).find((menu) => appPathForMenu(menu) === normalizedPath)
    if (direct) return direct

    const menuIdMatch = normalizedPath.match(MENU_ID_PATTERN)
    if (menuIdMatch) {
        return findMenuById(nodes, decodeURIComponent(menuIdMatch[1]))
    }

    return flattenMenus(nodes).find((menu) => {
        if (!menu.path?.includes(':')) return false
        return matchRoutePattern(normalizeAppPath(menu.path), normalizedPath) !== null
    }) || null
}

export function resolveTopModuleIdByPath(routeTree = [], pathname) {
    const active = findMenuByAppPath(routeTree, pathname)
    if (!active) return routeTree[0]?.id
    for (const top of routeTree) {
        if (top.id === active.id) return top.id
        if (findMenuById(top.children || [], active.id)) return top.id
    }
    return routeTree[0]?.id
}
