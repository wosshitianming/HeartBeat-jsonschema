import {safeStorageGet, safeStorageSet} from './infrastructure/browser/safeStorage'

const TENANT_STORAGE_KEY = 'heartbeat_tenant_id'

// 从本地会话中构造认证请求头（Bearer + 兼容 X-User-Id）
function sessionHeaders() {
  try {
    // 读取本地持久化的登录会话
      const session = JSON.parse(safeStorageGet('heartbeat_admin_session') || '{}')
    const headers = {}
    // 优先携带 JWT 访问令牌
    if (session.accessToken) {
      headers.Authorization = `Bearer ${session.accessToken}`
    }
    // 兼容旧版用户 ID 头
    if (session.userId) {
      headers['X-User-Id'] = String(session.userId)
    }
      const tenantId = session.tenantId || safeStorageGet(TENANT_STORAGE_KEY)
      if (tenantId) {
          headers['X-Tenant-Id'] = String(tenantId)
      }
    return headers
  } catch {
      const tenantId = safeStorageGet(TENANT_STORAGE_KEY)
      return tenantId ? {'X-Tenant-Id': String(tenantId)} : {}
  }
}

function unwrapRecordEnvelope(value) {
    if (!value || typeof value !== 'object' || Array.isArray(value)) return value
    const keys = Object.keys(value)
    if (keys.length !== 1 || keys[0] !== 'fields') return value
    const fields = value.fields
    return fields && typeof fields === 'object' && !Array.isArray(fields) ? fields : value
}

function normalizeResponseData(value) {
    return Array.isArray(value) ? value.map(unwrapRecordEnvelope) : unwrapRecordEnvelope(value)
}

// 统一 fetch 封装：按需附加 JSON 头与会话头，并解析 Result 包装
async function request(path, options = {}) {
    const {headers: optionHeaders, ...fetchOptions} = options
    const headers = {
        ...sessionHeaders(),
        ...(optionHeaders || {})
    }
    const hasContentType = Object.keys(headers).some((key) => key.toLowerCase() === 'content-type')
    const isFormData = typeof FormData !== 'undefined' && fetchOptions.body instanceof FormData
    if (fetchOptions.body != null && !isFormData && !hasContentType) {
        headers['Content-Type'] = 'application/json'
    }
  // 发起 HTTP 请求
  const response = await fetch(path, {
      ...fetchOptions,
      headers
  })

    let body = null
    if (response.status !== 204 && response.status !== 205) {
        if (typeof response.text === 'function') {
            const responseText = await response.text()
            if (responseText) {
                try {
                    body = JSON.parse(responseText)
                } catch {
                    body = responseText
                }
            }
        } else if (typeof response.json === 'function') {
            body = await response.json()
        }
    }

    if (!response.ok) {
        const message = typeof body === 'string' ? body : body?.msg
        throw new Error(message || `请求失败 (${response.status})`)
    }

    if (body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'code')) {
        if (String(body.code) !== '0') {
            throw new Error(body.msg || `请求失败 (${response.status})`)
        }
        return normalizeResponseData(body.data)
    }

    return normalizeResponseData(body)
}

export const structureApi = {
    preview(payload, options = {}) {
    return request('/api/v1/structure-definitions/preview', {
        ...options,
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  create(payload) {
    return request('/api/v1/structure-definitions', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
    list(options = {}) {
        return request('/api/v1/structure-definitions', options)
  },
  createVersion(id, payload) {
    return request(`/api/v1/structure-definitions/${id}/versions`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  saveDraft(id, payload) {
    return request(`/api/v1/structure-definitions/${id}/draft`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  copyVersionToDraft(id, versionNo) {
    return request(`/api/v1/structure-definitions/${id}/draft/from-version/${versionNo}`, {
      method: 'POST'
    })
  },
  createVersionFromDraft(id) {
    return request(`/api/v1/structure-definitions/${id}/versions/from-draft`, {
      method: 'POST'
    })
  },
  diff(id, params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        query.set(key, String(value))
      }
    })
    const suffix = query.toString() ? `?${query.toString()}` : ''
    return request(`/api/v1/structure-definitions/${id}/diff${suffix}`)
  },
  activate(id, versionNo) {
    return request(`/api/v1/structure-definitions/${id}/active-version`, {
      method: 'PUT',
      body: JSON.stringify({ versionNo })
    })
  },
  validate(id, payload) {
    return request(`/api/v1/structure-definitions/${id}/validate`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  }
}

export const adminApi = {
    modules(options = {}) {
        return request('/api/v1/admin/modules', options)
  },
    module(key, options = {}) {
        return request(`/api/v1/admin/modules/${key}`, options)
  },
    resources(resource, options = {}) {
        return request(`/api/v1/admin/resources/${resource}`, options)
  },
  createResource(resource, payload) {
    return request(`/api/v1/admin/resources/${resource}`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  updateResource(resource, id, payload) {
    return request(`/api/v1/admin/resources/${resource}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  deleteResource(resource, id) {
    return request(`/api/v1/admin/resources/${resource}/${id}`, {
      method: 'DELETE'
    })
  }
}

export const iamApi = {
    routes(options = {}) {
        return request('/api/v1/iam/routes', options)
  },
    menus(options = {}) {
        return request('/api/v1/iam/menus', options)
  },
  createMenu(payload) {
    return request('/api/v1/iam/menus', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  updateMenu(id, payload) {
    return request(`/api/v1/iam/menus/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  deleteMenu(id) {
    return request(`/api/v1/iam/menus/${id}`, {
      method: 'DELETE'
    })
  },
    menuTreeSelect(options = {}) {
        return request('/api/v1/iam/menus/tree-select', options)
  },
    roleMenus(roleId, options = {}) {
        return request(`/api/v1/iam/roles/${roleId}/menus`, options)
  },
  assignRoleMenus(roleId, menuIds) {
    return request(`/api/v1/iam/roles/${roleId}/menus`, {
      method: 'PUT',
      body: JSON.stringify({ menuIds })
    })
  }
}

export const monitorApi = {
  server() {
    return request('/api/v1/monitor/server')
  }
}

export const authApi = {
  me() {
    return request('/api/v1/auth/me')
  },
  login(payload) {
      const tenantId = String(payload?.tenantId || safeStorageGet(TENANT_STORAGE_KEY) || '1').trim() || '1'
      safeStorageSet(TENANT_STORAGE_KEY, tenantId)
      const credentials = {...(payload || {})}
      delete credentials.tenantId
    return request('/api/v1/auth/login', {
      method: 'POST',
        headers: {'X-Tenant-Id': tenantId},
        body: JSON.stringify(credentials)
    })
  },
  logout() {
    return request('/api/v1/auth/logout', {
      method: 'POST'
    })
  },
  refresh(refreshToken) {
    return request('/api/v1/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken })
    })
  },
  socialProviders() {
    return request('/api/v1/auth/social/providers')
  },
  socialAuthorize(provider, redirect = '/admin') {
    const query = new URLSearchParams({ redirect })
    return request(`/api/v1/auth/social/${provider}/authorize?${query.toString()}`)
  },
  socialCallback(provider, code, state) {
    const query = new URLSearchParams({ code, state })
    return request(`/api/v1/auth/social/${provider}/callback?${query.toString()}`)
  },
  socialBind(payload) {
    return request('/api/v1/auth/social/bind', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
    appearancePreference(options = {}) {
        return request('/api/v1/auth/preferences/appearance', options)
  },
    updateAppearancePreference(appearance, options = {}) {
    return request('/api/v1/auth/preferences/appearance', {
        ...options,
      method: 'PUT',
      body: JSON.stringify(appearance)
    })
  }
}

export const toolApi = {
  runJob(id) {
    return request(`/api/v1/tool/jobs/${id}/run`, { method: 'POST' })
  },
  pauseJob(id) {
    return request(`/api/v1/tool/jobs/${id}/pause`, { method: 'POST' })
  },
  resumeJob(id) {
    return request(`/api/v1/tool/jobs/${id}/resume`, { method: 'POST' })
  },
  refreshJobs() {
    return request('/api/v1/tool/jobs/refresh', { method: 'POST' })
  },
  listDbTables() {
    return request('/api/v1/tool/gen/tables')
  },
  listImportedTables() {
    return request('/api/v1/tool/gen/imported')
  },
  importTable(tableName) {
    const query = new URLSearchParams({ tableName })
    return request(`/api/v1/tool/gen/tables/import?${query.toString()}`, { method: 'POST' })
  },
  previewCodegen(tableId) {
    return request(`/api/v1/tool/gen/tables/${tableId}/preview`)
  },
  async downloadCodegen(tableId) {
    const response = await fetch(`/api/v1/tool/gen/tables/${tableId}/download`, {
      headers: sessionHeaders()
    })
    if (!response.ok) {
      throw new Error('代码下载失败')
    }
    return response.blob()
  }
}

export const flowApi = {
  components() {
    return request('/api/v1/flow/components')
  },
  listFlows() {
    return request('/api/v1/flows')
  },
  createFlow(payload) {
    return request('/api/v1/flows', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  saveDraft(id, payload) {
    return request(`/api/v1/flows/${id}/draft`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  compile(id, payload) {
    return request(`/api/v1/flows/${id || 'draft'}/compile`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  publish(id) {
    return request(`/api/v1/flows/${id}/publish`, { method: 'POST' })
  },
  debug(id, payload) {
    return request(`/api/v1/flows/${id}/debug`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  runs(id) {
    return request(`/api/v1/flows/${id}/runs`)
  },
  connections() {
    return request('/api/v1/flow/connections')
  },
  saveConnection(payload) {
    return request('/api/v1/flow/connections', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  }
}
