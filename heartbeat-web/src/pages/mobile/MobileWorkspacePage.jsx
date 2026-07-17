import {useCallback, useEffect, useMemo, useState} from 'react'
import {mobileApi} from '../../api'
import {
    BackendDataTable,
    MetricStrip,
    parseJsonField,
    RecordDialog,
    StatusBadge,
    WorkspaceHeader,
    WorkspaceTabs
} from '../../components/admin/BackendWorkspace'
import {hasPermission} from '../../domain/admin/permissionPolicy'
import './MobileWorkspacePage.css'

const VIEW_ITEMS = [
    {key: 'apps', label: '移动应用'},
    {key: 'pages', label: '页面配置'},
    {key: 'routes', label: 'API 路由'}
]

const APP_FIELDS = [
    {name: 'name', label: '应用名称', required: true},
    {name: 'appKey', label: '应用标识', required: true, placeholder: '例如 sales_mobile'},
    {name: 'entryUrl', label: '入口地址', placeholder: '/mobile/sales'},
    {
        name: 'status',
        label: '状态',
        type: 'select',
        options: ['DRAFT', 'PUBLISHED', 'DISABLED'],
        defaultValue: 'DRAFT'
    },
    {name: 'config', label: '应用配置', type: 'json', fullWidth: true, defaultValue: {}}
]

const PAGE_FIELDS = [
    {name: 'name', label: '页面名称', required: true},
    {name: 'pageKey', label: '页面标识', required: true, placeholder: '例如 order_list'},
    {name: 'routePath', label: '页面路由', required: true, placeholder: '/orders'},
    {name: 'sortNo', label: '排序', type: 'number', defaultValue: 0},
    {
        name: 'status',
        label: '状态',
        type: 'select',
        options: ['DRAFT', 'PUBLISHED', 'DISABLED'],
        defaultValue: 'DRAFT'
    },
    {name: 'schema', label: '页面 Schema', type: 'json', fullWidth: true, defaultValue: {}}
]

const ROUTE_FIELDS = [
    {name: 'name', label: '路由名称', required: true},
    {name: 'routeKey', label: '路由标识', required: true, placeholder: '例如 order_query'},
    {
        name: 'method',
        label: '请求方法',
        type: 'select',
        options: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
        defaultValue: 'GET'
    },
    {name: 'path', label: '前端路径', required: true, placeholder: '/api/orders'},
    {name: 'targetUrl', label: '目标地址', required: true, placeholder: 'https://service.example/api/orders'},
    {name: 'sortNo', label: '排序', type: 'number', defaultValue: 0},
    {name: 'status', label: '状态', type: 'select', options: ['ACTIVE', 'DISABLED'], defaultValue: 'ACTIVE'}
]

const APP_COLUMNS = [
    {key: 'name', label: '应用名称'},
    {key: 'appKey', label: '应用标识'},
    {key: 'entryUrl', label: '入口地址'},
    {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
    {key: 'createTime', label: '创建时间'},
    {key: 'updateTime', label: '更新时间'}
]

const PAGE_COLUMNS = [
    {key: 'name', label: '页面名称'},
    {key: 'pageKey', label: '页面标识'},
    {key: 'routePath', label: '页面路由'},
    {key: 'sortNo', label: '排序'},
    {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
    {key: 'updateTime', label: '更新时间'}
]

const ROUTE_COLUMNS = [
    {key: 'name', label: '路由名称'},
    {key: 'routeKey', label: '路由标识'},
    {key: 'method', label: '方法'},
    {key: 'path', label: '前端路径'},
    {key: 'targetUrl', label: '目标地址'},
    {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>}
]

function normalizeView(view) {
    if (view === 'api-routes') return 'routes'
    return VIEW_ITEMS.some((item) => item.key === view) ? view : 'apps'
}

export default function MobileWorkspacePage({initialView = 'apps', permissions = [], onError}) {
    const [activeView, setActiveView] = useState(() => normalizeView(initialView))
    const [apps, setApps] = useState([])
    const [selectedAppId, setSelectedAppId] = useState('')
    const [pages, setPages] = useState([])
    const [routes, setRoutes] = useState([])
    const [appsLoading, setAppsLoading] = useState(true)
    const [childrenLoading, setChildrenLoading] = useState(false)
    const [actionBusy, setActionBusy] = useState('')
    const [localError, setLocalError] = useState('')
    const [feedback, setFeedback] = useState('')
    const [dialog, setDialog] = useState('')
    const canEdit = hasPermission(permissions, 'biz:mobile:edit')

    useEffect(() => setActiveView(normalizeView(initialView)), [initialView])

    const reportError = useCallback((error) => {
        if (error?.name === 'AbortError') return
        const message = error?.message || '移动应用操作失败'
        setLocalError(message)
        onError?.(message)
    }, [onError])

    const fetchApps = useCallback(async (preferredId, options = {}) => {
        const items = await mobileApi.apps(options)
        const safeItems = Array.isArray(items) ? items : []
        setApps(safeItems)
        setSelectedAppId((current) => {
            const target = preferredId == null ? current : String(preferredId)
            if (target && safeItems.some((item) => String(item.id) === target)) return target
            return safeItems[0]?.id == null ? '' : String(safeItems[0].id)
        })
        return safeItems
    }, [])

    const fetchChildren = useCallback(async (appId, options = {}) => {
        if (!appId) {
            setPages([])
            setRoutes([])
            return
        }
        const results = await Promise.allSettled([
            mobileApi.pages(appId, options),
            mobileApi.apiRoutes(appId, options)
        ])
        if (options.signal?.aborted) return
        if (results[0].status === 'fulfilled') setPages(results[0].value || [])
        if (results[1].status === 'fulfilled') setRoutes(results[1].value || [])
        const rejected = results.find((result) => result.status === 'rejected' && result.reason?.name !== 'AbortError')
        if (rejected) throw rejected.reason
    }, [])

    useEffect(() => {
        const controller = new AbortController()
        setAppsLoading(true)
        fetchApps(undefined, {signal: controller.signal})
            .catch(reportError)
            .finally(() => {
                if (!controller.signal.aborted) setAppsLoading(false)
            })
        return () => controller.abort()
    }, [fetchApps, reportError])

    useEffect(() => {
        if (!selectedAppId) {
            setPages([])
            setRoutes([])
            return undefined
        }
        const controller = new AbortController()
        setChildrenLoading(true)
        fetchChildren(selectedAppId, {signal: controller.signal})
            .catch(reportError)
            .finally(() => {
                if (!controller.signal.aborted) setChildrenLoading(false)
            })
        return () => controller.abort()
    }, [fetchChildren, reportError, selectedAppId])

    const selectedApp = useMemo(
        () => apps.find((item) => String(item.id) === selectedAppId),
        [apps, selectedAppId]
    )

    const tabs = useMemo(() => {
        const counts = {apps: apps.length, pages: pages.length, routes: routes.length}
        return VIEW_ITEMS.map((item) => ({...item, count: counts[item.key]}))
    }, [apps.length, pages.length, routes.length])

    const loading = appsLoading || childrenLoading
    const publishedApps = apps.filter((item) => item.status === 'PUBLISHED').length
    const publishedPages = pages.filter((item) => item.status === 'PUBLISHED').length
    const activeRoutes = routes.filter((item) => item.status === 'ACTIVE').length

    async function runAction(key, action, successMessage, after, rethrow = false) {
        setActionBusy(key)
        setLocalError('')
        setFeedback('')
        try {
            const result = await action()
            await after?.(result)
            setDialog('')
            setFeedback(successMessage)
            return result
        } catch (error) {
            reportError(error)
            if (rethrow) throw error
            return null
        } finally {
            setActionBusy('')
        }
    }

    async function refreshAll() {
        setAppsLoading(true)
        setChildrenLoading(Boolean(selectedAppId))
        setLocalError('')
        setFeedback('')
        try {
            await fetchApps(selectedAppId)
            if (selectedAppId) await fetchChildren(selectedAppId)
        } catch (error) {
            reportError(error)
        } finally {
            setAppsLoading(false)
            setChildrenLoading(false)
        }
    }

    async function saveApp(values) {
        await runAction('save-app', () => mobileApi.saveApp({
            ...values,
            config: parseJsonField(values.config, {})
        }), '移动应用已保存', async (saved) => {
            await fetchApps(saved?.id || selectedAppId)
        }, true)
    }

    async function savePage(values) {
        await runAction('save-page', () => mobileApi.savePage({
            ...values,
            appId: selectedAppId,
            sortNo: Number(values.sortNo || 0),
            schema: parseJsonField(values.schema, {})
        }), '移动页面已保存', () => fetchChildren(selectedAppId), true)
    }

    async function saveRoute(values) {
        await runAction('save-route', () => mobileApi.saveApiRoute({
            ...values,
            appId: selectedAppId,
            sortNo: Number(values.sortNo || 0)
        }), 'API 路由已保存', () => fetchChildren(selectedAppId), true)
    }

    const headerActions = canEdit && activeView === 'apps' ? (
        <button className="button primary" type="button" onClick={() => setDialog('app')}>新增应用</button>
    ) : canEdit && selectedApp ? (
        <button className="button primary" type="button"
                onClick={() => setDialog(activeView === 'pages' ? 'page' : 'route')}>
            {activeView === 'pages' ? '新增页面' : '新增路由'}
        </button>
    ) : null

    return (
        <div className="hb-page-card backend-workspace-page mobile-workspace-page">
            <WorkspaceHeader
                breadcrumb="业务中心 / 移动搭建"
                title="移动应用搭建"
                description="集中管理移动应用、页面 Schema 与后端 API 路由。"
                status={selectedApp ? selectedApp.status : '真实数据'}
                loading={loading}
                onRefresh={refreshAll}
                actions={headerActions}
            />

            {localError && <div className="error-banner" role="alert">{localError}</div>}
            {feedback && <div className="mobile-feedback" role="status">{feedback}</div>}

            <WorkspaceTabs items={tabs} activeKey={activeView} onChange={setActiveView}/>

            <MetricStrip items={[
                {label: '移动应用', value: apps.length, hint: `${publishedApps} 个已发布`},
                {label: '当前页面', value: pages.length, hint: `${publishedPages} 个已发布`},
                {label: 'API 路由', value: routes.length, hint: `${activeRoutes} 条生效`},
                {label: '当前应用', value: selectedApp?.name || '未选择', hint: selectedApp?.appKey || '请选择应用'}
            ]}/>

            {apps.length > 0 && (
                <section className="mobile-app-context" aria-label="当前移动应用">
                    <div>
                        <span>当前应用</span>
                        <strong>{selectedApp?.name || '请选择应用'}</strong>
                        <small>{selectedApp?.entryUrl || selectedApp?.appKey || '尚未配置入口'}</small>
                    </div>
                    <label>
                        <span>切换应用</span>
                        <select value={selectedAppId} onChange={(event) => setSelectedAppId(event.target.value)}>
                            {apps.map((app) => (
                                <option key={app.id} value={app.id}>{app.name || app.appKey || app.id}</option>
                            ))}
                        </select>
                    </label>
                </section>
            )}

            {activeView === 'apps' && (
                <BackendDataTable
                    ariaLabel="移动应用列表"
                    columns={APP_COLUMNS}
                    rows={apps}
                    loading={appsLoading}
                    selectedId={selectedAppId}
                    onSelect={(row) => setSelectedAppId(String(row.id))}
                    emptyText="暂无移动应用"
                    searchPlaceholder="搜索应用名称、标识或入口"
                    rowActions={(row) => (
                        <button
                            className="table-link"
                            type="button"
                            onClick={() => {
                                setSelectedAppId(String(row.id))
                                setActiveView('pages')
                            }}
                        >
                            管理页面
                        </button>
                    )}
                />
            )}

            {activeView !== 'apps' && !selectedApp && (
                <section className="panel mobile-empty-context">
                    <strong>请先创建移动应用</strong>
                    <p>页面和 API 路由需要归属到具体应用。</p>
                    {canEdit && (
                        <button className="button primary" type="button"
                                onClick={() => setDialog('app')}>新增应用</button>
                    )}
                </section>
            )}

            {activeView === 'pages' && selectedApp && (
                <BackendDataTable
                    ariaLabel="移动页面列表"
                    columns={PAGE_COLUMNS}
                    rows={pages}
                    loading={childrenLoading}
                    emptyText="当前应用暂无页面"
                    searchPlaceholder="搜索页面名称、标识或路由"
                />
            )}

            {activeView === 'routes' && selectedApp && (
                <BackendDataTable
                    ariaLabel="移动 API 路由列表"
                    columns={ROUTE_COLUMNS}
                    rows={routes}
                    loading={childrenLoading}
                    emptyText="当前应用暂无 API 路由"
                    searchPlaceholder="搜索路由名称、路径或目标地址"
                />
            )}

            <RecordDialog
                open={dialog === 'app'}
                title="新增移动应用"
                description="应用保存为独立记录；发布状态会由后端同步生成应用版本。"
                fields={APP_FIELDS}
                initialValues={{name: '', appKey: '', entryUrl: '', status: 'DRAFT', config: {}}}
                submitLabel="保存应用"
                busy={actionBusy === 'save-app'}
                onClose={() => setDialog('')}
                onSubmit={saveApp}
            />

            <RecordDialog
                open={dialog === 'page'}
                title={`新增页面：${selectedApp?.name || ''}`}
                fields={PAGE_FIELDS}
                initialValues={{
                    name: '',
                    pageKey: '',
                    routePath: '',
                    sortNo: pages.length,
                    status: 'DRAFT',
                    schema: {}
                }}
                submitLabel="保存页面"
                busy={actionBusy === 'save-page'}
                onClose={() => setDialog('')}
                onSubmit={savePage}
            />

            <RecordDialog
                open={dialog === 'route'}
                title={`新增 API 路由：${selectedApp?.name || ''}`}
                fields={ROUTE_FIELDS}
                initialValues={{
                    name: '',
                    routeKey: '',
                    method: 'GET',
                    path: '',
                    targetUrl: '',
                    sortNo: routes.length,
                    status: 'ACTIVE'
                }}
                submitLabel="保存路由"
                busy={actionBusy === 'save-route'}
                onClose={() => setDialog('')}
                onSubmit={saveRoute}
            />
        </div>
    )
}
