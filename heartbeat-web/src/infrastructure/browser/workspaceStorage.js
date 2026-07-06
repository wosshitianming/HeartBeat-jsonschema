const STORAGE_KEY = 'heartbeat_admin_workspace'

export function readWorkspaceState(userId = 'anonymous', storage = window.localStorage) {
    try {
        const raw = storage.getItem(`${STORAGE_KEY}:${userId}`)
        if (!raw) return null
        const parsed = JSON.parse(raw)
        if (!Array.isArray(parsed.tags)) return null
        return parsed
    } catch {
        return null
    }
}

export function writeWorkspaceState(userId = 'anonymous', state, storage = window.localStorage) {
    if (!state) return
    try {
        storage.setItem(`${STORAGE_KEY}:${userId}`, JSON.stringify({
            activeKey: state.activeKey,
            tags: state.tags
        }))
    } catch {
        // Storage can be unavailable in private browsing; workspace can still run in memory.
    }
}

export function clearWorkspaceState(userId = 'anonymous', storage = window.localStorage) {
    try {
        storage.removeItem(`${STORAGE_KEY}:${userId}`)
    } catch {
        // Ignore storage errors.
    }
}
