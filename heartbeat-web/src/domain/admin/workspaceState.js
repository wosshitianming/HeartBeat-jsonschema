import {
    closeTargetForActiveTag,
    DEFAULT_ADMIN_PATH,
    tagKeyForLocation,
    updateTagTitle,
    upsertTagForLocation
} from './workspaceRouting'

export function createInitialWorkspaceState(location = {pathname: DEFAULT_ADMIN_PATH, search: ''}, meta = {}) {
    const key = tagKeyForLocation(location)
    return {
        activeKey: key,
        tags: upsertTagForLocation([], location, {
            closable: false,
            title: meta.title || meta.name || 'HeartBeat',
            name: meta.name || meta.title || 'HeartBeat',
            menuId: meta.menuId || 'structure'
        })
    }
}

export function activateWorkspaceLocation(state, location, meta = {}) {
    const key = tagKeyForLocation(location)
    return {
        ...state,
        activeKey: key,
        tags: upsertTagForLocation(state.tags, location, meta)
    }
}

export function closeWorkspaceTag(state, key) {
    const result = closeTargetForActiveTag(state.tags, state.activeKey, key)
    return {
        state: {
            ...state,
            activeKey: result.nextPath || state.activeKey,
            tags: result.tags
        },
        nextPath: result.nextPath,
        replace: result.replace
    }
}

export function renameWorkspaceTag(state, key, title) {
    return {
        ...state,
        tags: updateTagTitle(state.tags, key, title)
    }
}
