export const DEFAULT_ADMIN_PATH = '/admin/structure'

const DYNAMIC_SEGMENT_PATTERN = /^:([^/]+)$/

export function normalizePath(path = '/') {
    const value = String(path || '/').trim()
    if (!value || value === '/') return '/'
    const withSlash = value.startsWith('/') ? value : `/${value}`
    return withSlash.replace(/\/{2,}/g, '/').replace(/\/+$/g, '') || '/'
}

export function tagKeyForLocation(location = {}) {
    const pathname = normalizePath(location.pathname || DEFAULT_ADMIN_PATH)
    const search = location.search ? String(location.search) : ''
    return `${pathname}${search}`
}

export function splitPath(path) {
    return normalizePath(path)
        .split('/')
        .filter(Boolean)
}

export function matchRoutePattern(pattern, pathname) {
    const patternSegments = splitPath(pattern)
    const pathSegments = splitPath(pathname)
    if (patternSegments.length !== pathSegments.length) return null

    return patternSegments.reduce((params, segment, index) => {
        if (params === null) return null
        const dynamic = segment.match(DYNAMIC_SEGMENT_PATTERN)
        if (dynamic) {
            return {
                ...params,
                [dynamic[1]]: decodeURIComponent(pathSegments[index])
            }
        }
        return segment === pathSegments[index] ? params : null
    }, {})
}

export function upsertTagForLocation(tags = [], location = {}, meta = {}) {
    const key = tagKeyForLocation(location)
    const existing = tags.find((tag) => tag.key === key || tag.id === key)
    const nextTag = {
        id: key,
        key,
        path: key,
        title: meta.title || meta.name || existing?.title || existing?.name || key,
        name: meta.name || meta.title || existing?.name || existing?.title || key,
        menuId: meta.menuId ?? existing?.menuId,
        closable: meta.closable ?? existing?.closable ?? true,
        params: meta.params || existing?.params || {}
    }

    if (existing) {
        return tags.map((tag) => (tag === existing ? {...tag, ...nextTag} : tag))
    }
    return [...tags, nextTag]
}

export function updateTagTitle(tags = [], key, title) {
    if (!key || !title) return tags
    return tags.map((tag) => (
        tag.key === key || tag.id === key
            ? {...tag, title, name: title}
            : tag
    ))
}

export function closeTargetForActiveTag(tags = [], activeKey, closingKey) {
    const targetKey = closingKey || activeKey
    const currentIndex = tags.findIndex((tag) => tag.key === targetKey || tag.id === targetKey)
    const closingTag = tags[currentIndex]
    if (!closingTag || closingTag.closable === false) {
        return {
            tags,
            nextPath: null,
            replace: false
        }
    }

    const nextTags = tags.filter((tag) => tag !== closingTag)
    const isClosingActive = targetKey === activeKey
    if (!isClosingActive) {
        return {
            tags: nextTags,
            nextPath: null,
            replace: false
        }
    }

    const left = nextTags[Math.max(0, currentIndex - 1)]
    const right = nextTags[currentIndex]
    const target = left || right || nextTags[0]

    return {
        tags: nextTags,
        nextPath: target?.path || target?.key || DEFAULT_ADMIN_PATH,
        replace: true
    }
}
