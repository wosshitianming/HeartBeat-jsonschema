export const DENSE_TABLE_ROW_THRESHOLD = 50

const SURFACE_ORDER = {
    flat: 0,
    restrained: 1,
    balanced: 2,
    immersive: 3,
    glass: 2
}

function clampSurfaceMode(mode, maxMode) {
    const normalized = SURFACE_ORDER[mode] === undefined ? 'balanced' : mode
    return SURFACE_ORDER[normalized] > SURFACE_ORDER[maxMode] ? maxMode : normalized
}

export function effectiveSurfaceMode({
                                         manualMode = 'balanced',
                                         supportsBackdrop = true,
                                         reducedMotion = false,
                                         rowCount = 0
                                     } = {}) {
    if (!supportsBackdrop) return 'flat'
    if (reducedMotion) return clampSurfaceMode(manualMode, 'restrained')
    if (Number(rowCount) > DENSE_TABLE_ROW_THRESHOLD) {
        return clampSurfaceMode(manualMode, 'restrained')
    }
    return SURFACE_ORDER[manualMode] === undefined ? 'balanced' : manualMode
}

export function detectBackdropSupport(globalObject = globalThis) {
    const css = globalObject?.CSS
    if (!css?.supports) return false
    return css.supports('backdrop-filter', 'blur(1px)')
        || css.supports('-webkit-backdrop-filter', 'blur(1px)')
}
