function resolveStorage(storage) {
    if (storage) return storage
    try {
        return window.localStorage
    } catch {
        return null
    }
}

export function safeStorageGet(key, storage) {
    try {
        return resolveStorage(storage)?.getItem(key) ?? null
    } catch {
        return null
    }
}

export function safeStorageSet(key, value, storage) {
    try {
        const target = resolveStorage(storage)
        if (!target) return false
        target.setItem(key, value)
        return true
    } catch {
        return false
    }
}

export function safeStorageRemove(key, storage) {
    try {
        const target = resolveStorage(storage)
        if (!target) return false
        target.removeItem(key)
        return true
    } catch {
        return false
    }
}
