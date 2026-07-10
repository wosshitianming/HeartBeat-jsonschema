import {safeStorageGet, safeStorageRemove, safeStorageSet} from './safeStorage'

const STORAGE_KEY = 'heartbeat_admin_workspace'

export function readWorkspaceState(userId = 'anonymous', storage) {
    try {
        const raw = safeStorageGet(`${STORAGE_KEY}:${userId}`, storage)
        if (!raw) return null
        const parsed = JSON.parse(raw)
        if (!Array.isArray(parsed.tags)) return null
        return parsed
    } catch {
        return null
    }
}

export function writeWorkspaceState(userId = 'anonymous', state, storage) {
    if (!state) return
    safeStorageSet(`${STORAGE_KEY}:${userId}`, JSON.stringify({
        activeKey: state.activeKey,
        tags: state.tags
    }), storage)
}

export function clearWorkspaceState(userId = 'anonymous', storage) {
    safeStorageRemove(`${STORAGE_KEY}:${userId}`, storage)
}
