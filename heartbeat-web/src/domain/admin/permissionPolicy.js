function asPermissionSet(source) {
    const permissions = Array.isArray(source) ? source : source?.permissions
    return new Set((permissions || []).map((item) => String(item)))
}

export function hasPermission(source, permission) {
    if (!permission) return true
    const permissions = asPermissionSet(source)
    return permissions.has('*')
        || permissions.has('*:*')
        || permissions.has(permission)
}

export function hasAnyPermission(source, required = []) {
    return required.some((permission) => hasPermission(source, permission))
}

export function hasAllPermissions(source, required = []) {
    return required.every((permission) => hasPermission(source, permission))
}
