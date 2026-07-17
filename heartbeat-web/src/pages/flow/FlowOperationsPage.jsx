import {useCallback, useEffect, useMemo, useRef, useState} from 'react'
import {ArrowDownToLine, ArrowUpFromLine, Box, Code2, X} from 'lucide-react'
import {useNavigate} from 'react-router-dom'
import {flowApi} from '../../api'
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
import './FlowOperationsPage.css'

const TAB_ITEMS = [
    {key: 'definitions', label: '流程定义'},
    {key: 'components', label: '节点组件'},
    {key: 'connections', label: '连接凭据'},
    {key: 'runs', label: '运行记录'}
]

const CONNECTION_FIELDS = [
    {name: 'name', label: '凭据名称', required: true},
    {name: 'type', label: '连接类型', required: true, placeholder: 'HTTP / JDBC / REDIS / CUSTOM'},
    {
        name: 'status',
        label: '状态',
        type: 'select',
        options: ['ACTIVE'],
        defaultValue: 'ACTIVE',
        disabled: true,
        hint: '新建连接凭据默认直接启用。'
    },
    {name: 'config', label: '公开配置', type: 'json', fullWidth: true, defaultValue: {}},
    {
        name: 'secrets',
        label: '敏感配置',
        type: 'json',
        fullWidth: true,
        defaultValue: {},
        hint: '后端列表仅返回脱敏值，编辑时需重新填写已有密钥。'
    }
]

const RUN_FIELDS = [
    {name: 'variables', label: '运行变量', type: 'json', fullWidth: true, defaultValue: {}}
]

const CANCEL_FIELDS = [
    {name: 'reason', label: '取消原因', type: 'textarea', fullWidth: true, required: true}
]

const CANCELABLE_RUN_STATUSES = new Set(['CREATED', 'RUNNING', 'WAITING', 'PROCESSING'])

function formatDuration(value) {
    const milliseconds = Number(value)
    if (!Number.isFinite(milliseconds)) return '—'
    if (milliseconds < 1000) return `${milliseconds} ms`
    return `${(milliseconds / 1000).toFixed(milliseconds < 10000 ? 1 : 0)} s`
}

function formatJson(value) {
    try {
        return JSON.stringify(value ?? {}, null, 2)
    } catch {
        return String(value ?? '')
    }
}

function parseJsonObject(value, label, fallback = {}) {
    const parsed = parseJsonField(value, fallback)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        throw new Error(`${label}必须是 JSON 对象`)
    }
    return parsed
}

function portCount(component) {
    const inputs = Array.isArray(component?.ports?.inputs) ? component.ports.inputs.length : 0
    const outputs = Array.isArray(component?.ports?.outputs) ? component.ports.outputs.length : 0
    return `${inputs} 入 / ${outputs} 出`
}

function componentKey(component) {
    return component?.id || `${component?.type || 'component'}@${component?.version || '1.0.0'}`
}

function componentSourceLabel(source) {
    const labels = {
        BUILTIN: '系统内置',
        CODE: '代码节点',
        DATABASE: '声明清单'
    }
    return labels[String(source || '').toUpperCase()] || source || '未知来源'
}

function ComponentPorts({title, icon: Icon, items = [], emptyText}) {
    return (
        <div className="flow-component-port-group">
            <header><Icon size={15} aria-hidden="true"/><strong>{title}</strong><span>{items.length}</span></header>
            {items.length ? items.map((port) => (
                <div className="flow-component-port" key={port.id}>
                    <code>{port.id}</code>
                    <span>{port.label || port.id}</span>
                    <small>{port.schema || 'object'}{port.required ? ' · 必填' : ''}</small>
                </div>
            )) : <div className="flow-component-port-empty">{emptyText}</div>}
        </div>
    )
}

function ComponentContract({component, onClose}) {
    if (!component) return null
    const runtime = component.runtime || {}
    const inputs = Array.isArray(component.ports?.inputs) ? component.ports.inputs : []
    const outputs = Array.isArray(component.ports?.outputs) ? component.ports.outputs : []
    const codeNode = String(component.source || '').toUpperCase() === 'CODE'
    return (
        <section className="flow-component-contract" aria-label={`${component.name || component.type} 节点契约`}>
            <header className="flow-component-contract-header">
                <div className="flow-component-contract-title">
            <span className="flow-component-contract-icon">
              {codeNode ? <Code2 size={18} aria-hidden="true"/> : <Box size={18} aria-hidden="true"/>}
            </span>
                    <div>
                        <span>{componentSourceLabel(component.source)}</span>
                        <h2>{component.name || component.type}</h2>
                        <p>{component.description || '—'}</p>
                    </div>
                </div>
                <button type="button" onClick={onClose} title="关闭节点详情" aria-label="关闭节点详情">
                    <X size={17} aria-hidden="true"/>
                </button>
            </header>

            <div className="flow-component-contract-meta">
                <div><span>组件类型</span><code>{component.type || '—'}</code></div>
                <div><span>版本</span><strong>v{component.version || '1.0.0'}</strong></div>
                <div><span>执行器</span><code>{runtime.executor || '未绑定'}</code></div>
                <div><span>运行模式</span><strong>{(runtime.mode || []).join(' / ') || '—'}</strong></div>
            </div>

            <div className="flow-component-contract-body">
                <section className="flow-component-contract-ports">
                    <h3>端口定义</h3>
                    <div>
                        <ComponentPorts title="输入" icon={ArrowDownToLine} items={inputs} emptyText="无输入端口"/>
                        <ComponentPorts title="输出" icon={ArrowUpFromLine} items={outputs} emptyText="无输出端口"/>
                    </div>
                </section>
                <section className="flow-component-contract-schema">
                    <h3>配置 Schema</h3>
                    <pre>{formatJson(component.configSchema || {})}</pre>
                </section>
            </div>

            {(runtime.capabilities || []).length > 0 && (
                <footer className="flow-component-capabilities">
                    <span>能力</span>
                    {(runtime.capabilities || []).map((capability) => <code key={capability}>{capability}</code>)}
                </footer>
            )}
        </section>
    )
}

function secretTemplate(connection) {
    return Object.keys(connection?.secrets || {}).reduce((result, key) => {
        result[key] = ''
        return result
    }, {})
}

function EventTimeline({items, emptyText}) {
    if (!items.length) return <div className="flow-events-empty">{emptyText}</div>
    return (
        <div className="flow-event-list">
            {items.map((event, index) => (
                <article className="flow-event-item" key={event.id || `${event.eventSeq}-${index}`}>
                    <header>
                        <div>
                            <strong>{event.eventType || '运行事件'}</strong>
                            <span>#{event.eventSeq ?? index + 1}</span>
                        </div>
                        <time>{event.createTime || '—'}</time>
                    </header>
                    <div className="flow-event-meta">
                        <span>节点 {event.nodeId || event.engineActivityId || '—'}</span>
                        <span>类型 {event.nodeType || '—'}</span>
                        <span>耗时 {formatDuration(event.elapsedMs)}</span>
                        {event.attemptNo != null && <span>尝试 {event.attemptNo}</span>}
                    </div>
                    {event.errorMessage && <p className="flow-event-error">{event.errorMessage}</p>}
                    {(Object.keys(event.input || {}).length > 0 || Object.keys(event.output || {}).length > 0) && (
                        <div className="flow-event-json-grid">
                            <div>
                                <span>输入</span>
                                <pre>{formatJson(event.input)}</pre>
                            </div>
                            <div>
                                <span>输出</span>
                                <pre>{formatJson(event.output)}</pre>
                            </div>
                        </div>
                    )}
                </article>
            ))}
        </div>
    )
}

export default function FlowOperationsPage({initialView = 'definitions', permissions = [], onError}) {
    const navigate = useNavigate()
    const [activeView, setActiveView] = useState(initialView)
    const [definitions, setDefinitions] = useState([])
    const [components, setComponents] = useState([])
    const [selectedComponentKey, setSelectedComponentKey] = useState('')
    const [connections, setConnections] = useState([])
    const [runs, setRuns] = useState([])
    const [selectedFlowId, setSelectedFlowId] = useState('')
    const [loading, setLoading] = useState(true)
    const [runsLoading, setRunsLoading] = useState(false)
    const [inspectionLoading, setInspectionLoading] = useState(false)
    const [actionBusy, setActionBusy] = useState('')
    const [localError, setLocalError] = useState('')
    const [dialog, setDialog] = useState({type: '', row: null})
    const [connectionTest, setConnectionTest] = useState(null)
    const [inspection, setInspection] = useState({run: null, events: [], replay: [], view: 'events'})
    const runsControllerRef = useRef(null)
    const runsRequestRef = useRef(0)
    const canEditConnections = hasPermission(permissions, 'flow:definition:edit')
    const canPublish = hasPermission(permissions, 'flow:definition:publish')

    useEffect(() => setActiveView(initialView), [initialView])

    useEffect(() => () => runsControllerRef.current?.abort(), [])

    const reportError = useCallback((error) => {
        const message = error?.message || String(error || '流程运维操作失败')
        setLocalError(message)
        onError?.(message)
    }, [onError])

    const loadCatalog = useCallback(async () => {
        setLoading(true)
        setLocalError('')
        const results = await Promise.allSettled([
            flowApi.listFlows(),
            flowApi.components(),
            flowApi.connections()
        ])
        const [flowResult, componentResult, connectionResult] = results

        if (flowResult.status === 'fulfilled') {
            const items = flowResult.value || []
            setDefinitions(items)
            setSelectedFlowId((current) => (
                items.some((item) => String(item.id) === String(current)) ? current : (items[0]?.id || '')
            ))
        }
        if (componentResult.status === 'fulfilled') {
            const items = componentResult.value || []
            setComponents(items)
            setSelectedComponentKey((current) => (
                items.some((item) => componentKey(item) === current) ? current : ''
            ))
        }
        if (connectionResult.status === 'fulfilled') setConnections(connectionResult.value || [])

        const rejected = results.find((result) => result.status === 'rejected')
        if (rejected) reportError(rejected.reason)
        setLoading(false)
    }, [reportError])

    const loadRuns = useCallback(async (flowId) => {
        runsControllerRef.current?.abort()
        const requestId = runsRequestRef.current + 1
        runsRequestRef.current = requestId
        if (!flowId) {
            setRuns([])
            setInspection({run: null, events: [], replay: [], view: 'events'})
            setRunsLoading(false)
            return
        }
        const controller = new AbortController()
        runsControllerRef.current = controller
        setRunsLoading(true)
        try {
            const items = await flowApi.runs(flowId, {signal: controller.signal})
            if (controller.signal.aborted || runsRequestRef.current !== requestId) return
            setRuns(items || [])
            setInspection((current) => (
                current.run && (items || []).some((item) => String(item.id) === String(current.run.id))
                    ? current
                    : {run: null, events: [], replay: [], view: 'events'}
            ))
        } catch (error) {
            if (controller.signal.aborted || error?.name === 'AbortError') return
            reportError(error)
        } finally {
            if (runsControllerRef.current === controller) runsControllerRef.current = null
            if (runsRequestRef.current === requestId) setRunsLoading(false)
        }
    }, [reportError])

    useEffect(() => {
        loadCatalog()
    }, [loadCatalog])

    useEffect(() => {
        setInspection({run: null, events: [], replay: [], view: 'events'})
        loadRuns(selectedFlowId)
    }, [loadRuns, selectedFlowId])

    const refreshWorkspace = useCallback(async () => {
        await Promise.all([
            loadCatalog(),
            selectedFlowId ? loadRuns(selectedFlowId) : Promise.resolve()
        ])
    }, [loadCatalog, loadRuns, selectedFlowId])

    const selectedFlow = useMemo(
        () => definitions.find((item) => String(item.id) === String(selectedFlowId)) || null,
        [definitions, selectedFlowId]
    )
    const selectedComponent = useMemo(
        () => components.find((item) => componentKey(item) === selectedComponentKey) || null,
        [components, selectedComponentKey]
    )

    const tabs = useMemo(() => {
        const counts = {
            definitions: definitions.length,
            components: components.length,
            connections: connections.length,
            runs: runs.length
        }
        return TAB_ITEMS.map((item) => ({...item, count: counts[item.key]}))
    }, [components.length, connections.length, definitions.length, runs.length])

    const activeConnections = connections.filter((item) => item.status === 'ACTIVE').length
    const publishedDefinitions = definitions.filter((item) => Number(item.activeVersionNo) > 0).length
    const runningRuns = runs.filter((item) => CANCELABLE_RUN_STATUSES.has(item.status)).length
    const codeComponents = components.filter((item) => String(item.source || '').toUpperCase() === 'CODE').length

    async function execute(key, action, options = {}) {
        setActionBusy(key)
        setLocalError('')
        try {
            const result = await action()
            if (options.refresh) await options.refresh()
            if (options.closeDialog) setDialog({type: '', row: null})
            return result
        } catch (error) {
            reportError(error)
            if (options.rethrow) throw error
            return null
        } finally {
            setActionBusy('')
        }
    }

    async function inspectRun(row) {
        if (!row?.id) return
        setInspectionLoading(true)
        setLocalError('')
        try {
            const [run, events] = await Promise.all([
                flowApi.runDetail(row.id),
                flowApi.runEvents(row.id)
            ])
            setInspection({run: run || row, events: events || [], replay: [], view: 'events'})
        } catch (error) {
            reportError(error)
        } finally {
            setInspectionLoading(false)
        }
    }

    async function replayRun(row) {
        if (!row?.id) return
        setInspectionLoading(true)
        setLocalError('')
        try {
            const [run, replay] = await Promise.all([
                flowApi.runDetail(row.id),
                flowApi.replayRun(row.id)
            ])
            setInspection({run: run || row, events: [], replay: replay || [], view: 'replay'})
        } catch (error) {
            reportError(error)
        } finally {
            setInspectionLoading(false)
        }
    }

    async function saveConnection(values) {
        const row = dialog.row
        const rowId = row?.id
        await execute(`connection-${rowId || 'create'}`, async () => {
            const secrets = parseJsonObject(values.secrets, '敏感配置', {})
            const existingSecretKeys = Object.keys(row?.secrets || {})
            const missingSecrets = existingSecretKeys.filter((key) => !String(secrets?.[key] ?? '').trim())
            if (missingSecrets.length > 0) {
                throw new Error(`请重新填写敏感配置：${missingSecrets.join('、')}`)
            }
            const payload = {
                ...values,
                config: parseJsonObject(values.config, '公开配置', {}),
                secrets
            }
            return rowId
                ? flowApi.updateConnection(rowId, payload)
                : flowApi.saveConnection(payload)
        }, {refresh: loadCatalog, closeDialog: true, rethrow: true})
    }

    async function testConnection(row) {
        const result = await execute(`test-connection-${row.id}`, () => flowApi.testConnection(row.id))
        if (result) setConnectionTest({...result, connectionName: row.name})
    }

    async function deleteConnection(row) {
        if (!window.confirm(`确定删除连接凭据“${row.name || row.id}”？`)) return
        await execute(`delete-connection-${row.id}`, () => flowApi.deleteConnection(row.id), {
            refresh: loadCatalog
        })
    }

    async function startRun(values) {
        await execute(`run-${selectedFlowId}`, () => (
            flowApi.run(selectedFlowId, parseJsonObject(values.variables, '运行变量', {}))
        ), {
            refresh: () => loadRuns(selectedFlowId),
            closeDialog: true,
            rethrow: true
        })
    }

    async function cancelRun(values) {
        const runId = dialog.row?.id
        const cancelled = await execute(`cancel-${runId}`, () => flowApi.cancelRun(runId, values.reason), {
            refresh: () => loadRuns(selectedFlowId),
            closeDialog: true,
            rethrow: true
        })
        if (cancelled && runId) {
            const refreshed = await flowApi.runDetail(runId).catch(() => null)
            if (refreshed) setInspection((current) => ({...current, run: refreshed}))
        }
    }

    const definitionColumns = [
        {key: 'name', label: '流程名称'},
        {key: 'code', label: '流程编码'},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'activeVersionNo', label: '活动版本', render: (value) => value ? `v${value}` : '未发布'},
        {key: 'runtimeEngine', label: '运行引擎'},
        {key: 'updateTime', label: '更新时间'}
    ]
    const componentColumns = [
        {key: 'name', label: '组件名称'},
        {key: 'type', label: '组件类型'},
        {key: 'category', label: '分类'},
        {key: 'version', label: '版本', render: (value) => `v${value || '1.0.0'}`},
        {key: 'source', label: '来源', render: componentSourceLabel},
        {key: 'runtime', label: '执行器', value: (row) => row.runtime?.executor || '未绑定'},
        {key: 'ports', label: '端口', value: portCount},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>}
    ]
    const connectionColumns = [
        {key: 'name', label: '凭据名称'},
        {key: 'type', label: '连接类型'},
        {key: 'config', label: '配置项', value: (row) => Object.keys(row.config || {}).length},
        {key: 'secrets', label: '密钥项', value: (row) => Object.keys(row.secrets || {}).length},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'updateTime', label: '更新时间'}
    ]
    const runColumns = [
        {key: 'runNo', label: '运行编号'},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'versionNo', label: '版本', render: (value) => value ? `v${value}` : '—'},
        {key: 'engine', label: '执行引擎'},
        {key: 'triggerType', label: '触发方式'},
        {key: 'startedAt', label: '开始时间'},
        {key: 'elapsedMs', label: '耗时', render: formatDuration}
    ]

    const headerAction = activeView === 'connections' && canEditConnections ? (
        <button
            className="button primary"
            type="button"
            disabled={Boolean(actionBusy)}
            onClick={() => setDialog({type: 'connection', row: null})}
        >
            新增凭据
        </button>
    ) : activeView === 'runs' && selectedFlowId && canPublish ? (
        <button
            className="button primary"
            type="button"
            disabled={Boolean(actionBusy) || !selectedFlow?.activeVersionNo}
            title={selectedFlow?.activeVersionNo ? '运行当前活动版本' : '请先发布流程'}
            onClick={() => setDialog({type: 'run', row: selectedFlow})}
        >
            运行流程
        </button>
    ) : null

    return (
        <div className="hb-page-card backend-workspace-page flow-operations-page">
            <WorkspaceHeader
                breadcrumb="数据与自动化 / 流程自动化"
                title="Flow 运维中心"
                description="集中查看流程定义、节点组件、连接凭据与运行轨迹，发布、连通性校验和运行控制直接调用 Flow 后端。"
                status="已接入"
                loading={loading || runsLoading || Boolean(actionBusy)}
                onRefresh={refreshWorkspace}
                actions={headerAction}
            />

            {localError && <div className="error-banner" role="alert">{localError}</div>}

            <WorkspaceTabs items={tabs} activeKey={activeView} onChange={setActiveView}/>
            <MetricStrip items={[
                {label: '流程定义', value: definitions.length, hint: `${publishedDefinitions} 个已发布`},
                {label: '节点组件', value: components.length, hint: `${codeComponents} 个代码节点`},
                {label: '连接凭据', value: activeConnections, hint: `共 ${connections.length} 个凭据`},
                {label: '当前流程运行', value: runs.length, hint: `${runningRuns} 个未结束`}
            ]}/>

            {activeView === 'definitions' && (
                <BackendDataTable
                    ariaLabel="流程定义列表"
                    columns={definitionColumns}
                    rows={definitions}
                    loading={loading}
                    emptyText="暂无流程定义，可先在流程设计器中创建草稿"
                    searchPlaceholder="搜索流程名称或编码"
                    actionColumnWidth={212}
                    rowActions={(row) => (
                        <>
                            <button
                                className="table-link"
                                type="button"
                                onClick={() => navigate(`/admin/flows/studio?flowId=${encodeURIComponent(row.id)}`)}
                            >
                                查看流程
                            </button>
                            {canPublish && (
                                <button
                                    className="table-link"
                                    type="button"
                                    disabled={Boolean(actionBusy)}
                                    onClick={() => execute(`publish-${row.id}`, () => flowApi.publish(row.id), {refresh: loadCatalog})}
                                >
                                    发布
                                </button>
                            )}
                            <button
                                className="table-link"
                                type="button"
                                disabled={Boolean(actionBusy)}
                                onClick={() => {
                                    setSelectedFlowId(row.id)
                                    setActiveView('runs')
                                }}
                            >
                                运行记录
                            </button>
                        </>
                    )}
                />
            )}

            {activeView === 'components' && (
                <>
                    <BackendDataTable
                        ariaLabel="节点组件列表"
                        columns={componentColumns}
                        rows={components}
                        loading={loading}
                        emptyText="暂无启用的节点组件"
                        searchPlaceholder="搜索组件名称、类型、执行器或分类"
                        rowKey={componentKey}
                        selectedId={selectedComponentKey}
                        onSelect={(row) => setSelectedComponentKey(componentKey(row))}
                    />
                    <ComponentContract component={selectedComponent} onClose={() => setSelectedComponentKey('')}/>
                </>
            )}

            {activeView === 'connections' && (
                <>
                    {connectionTest && (
                        <div className="flow-connection-test" data-success={connectionTest.success === true}>
                            <StatusBadge value={connectionTest.success ? 'SUCCESS' : 'FAILED'}/>
                            <div>
                                <strong>{connectionTest.connectionName || connectionTest.type}</strong>
                                <span>{connectionTest.message || '连接测试已完成'}</span>
                            </div>
                        </div>
                    )}
                    <BackendDataTable
                        ariaLabel="连接凭据列表"
                        columns={connectionColumns}
                        rows={connections}
                        loading={loading}
                        emptyText="暂无连接凭据"
                        searchPlaceholder="搜索凭据名称或类型"
                        rowActions={canEditConnections ? (row) => (
                            <>
                                <button
                                    className="table-link"
                                    type="button"
                                    disabled={Boolean(actionBusy)}
                                    onClick={() => testConnection(row)}
                                >
                                    测试
                                </button>
                                <button
                                    className="table-link"
                                    type="button"
                                    disabled={Boolean(actionBusy)}
                                    onClick={() => setDialog({type: 'connection', row})}
                                >
                                    编辑
                                </button>
                                <button
                                    className="table-link danger-text"
                                    type="button"
                                    disabled={Boolean(actionBusy)}
                                    onClick={() => deleteConnection(row)}
                                >
                                    删除
                                </button>
                            </>
                        ) : undefined}
                    />
                </>
            )}

            {activeView === 'runs' && (
                <>
                    <div className="flow-run-selector">
                        <label>
                            <span>流程定义</span>
                            <select
                                value={selectedFlowId}
                                disabled={Boolean(actionBusy) || definitions.length === 0}
                                onChange={(event) => setSelectedFlowId(event.target.value)}
                            >
                                {definitions.length === 0 && <option value="">暂无可选流程</option>}
                                {definitions.map((item) => (
                                    <option key={item.id} value={item.id}>{item.name} ({item.code})</option>
                                ))}
                            </select>
                        </label>
                        <div>
                            <strong>{selectedFlow?.name || '未选择流程'}</strong>
                            <span>
                    {selectedFlow
                        ? `${selectedFlow.code || '—'} · 活动版本 ${selectedFlow.activeVersionNo ? `v${selectedFlow.activeVersionNo}` : '未发布'}`
                        : '请先创建流程定义'}
                  </span>
                        </div>
                    </div>

                    <BackendDataTable
                        ariaLabel="流程运行记录"
                        columns={runColumns}
                        rows={runs}
                        loading={runsLoading}
                        emptyText={selectedFlowId ? '当前流程暂无运行记录' : '请先选择流程'}
                        searchPlaceholder="搜索运行编号、状态或触发方式"
                        selectedId={inspection.run?.id}
                        onSelect={inspectRun}
                        rowActions={(row) => (
                            <>
                                <button
                                    className="table-link"
                                    type="button"
                                    disabled={Boolean(actionBusy) || inspectionLoading}
                                    onClick={() => inspectRun(row)}
                                >
                                    详情
                                </button>
                                <button
                                    className="table-link"
                                    type="button"
                                    disabled={Boolean(actionBusy) || inspectionLoading}
                                    onClick={() => replayRun(row)}
                                >
                                    回放
                                </button>
                                {canPublish && CANCELABLE_RUN_STATUSES.has(row.status) && (
                                    <button
                                        className="table-link danger-text"
                                        type="button"
                                        disabled={Boolean(actionBusy)}
                                        onClick={() => setDialog({type: 'cancel', row})}
                                    >
                                        取消
                                    </button>
                                )}
                            </>
                        )}
                    />

                    {inspection.run && (
                        <section className="flow-run-inspector" aria-busy={inspectionLoading}>
                            <header className="flow-run-inspector-header">
                                <div>
                                    <span>运行详情</span>
                                    <h2>{inspection.run.runNo || inspection.run.id}</h2>
                                </div>
                                <StatusBadge value={inspection.run.status}/>
                            </header>
                            <div className="flow-run-detail-grid">
                                <div><span>流程版本</span><strong>v{inspection.run.versionNo || '—'}</strong></div>
                                <div><span>执行引擎</span><strong>{inspection.run.engine || '—'}</strong></div>
                                <div><span>触发方式</span><strong>{inspection.run.triggerType || '—'}</strong></div>
                                <div><span>运行耗时</span><strong>{formatDuration(inspection.run.elapsedMs)}</strong>
                                </div>
                                <div><span>开始时间</span><strong>{inspection.run.startedAt || '—'}</strong></div>
                                <div><span>结束时间</span><strong>{inspection.run.finishedAt || '—'}</strong></div>
                                <div><span>业务标识</span><strong>{inspection.run.businessKey || '—'}</strong></div>
                                <div><span>重试次数</span><strong>{inspection.run.retryNo ?? 0}</strong></div>
                            </div>
                            {inspection.run.errorMessage && (
                                <div className="flow-run-error" role="alert">{inspection.run.errorMessage}</div>
                            )}
                            <div className="flow-inspector-tabs" role="tablist" aria-label="运行轨迹视图">
                                <button
                                    type="button"
                                    role="tab"
                                    className={inspection.view === 'events' ? 'active' : ''}
                                    aria-selected={inspection.view === 'events'}
                                    disabled={Boolean(actionBusy)}
                                    onClick={() => setInspection((current) => ({...current, view: 'events'}))}
                                >
                                    事件 {inspection.events.length}
                                </button>
                                <button
                                    type="button"
                                    role="tab"
                                    className={inspection.view === 'replay' ? 'active' : ''}
                                    aria-selected={inspection.view === 'replay'}
                                    disabled={Boolean(actionBusy) || inspectionLoading}
                                    onClick={() => (
                                        inspection.replay.length > 0
                                            ? setInspection((current) => ({...current, view: 'replay'}))
                                            : replayRun(inspection.run)
                                    )}
                                >
                                    回放轨迹 {inspection.replay.length}
                                </button>
                            </div>
                            {inspectionLoading ? (
                                <div className="flow-events-empty">正在加载运行轨迹...</div>
                            ) : inspection.view === 'replay' ? (
                                <EventTimeline items={inspection.replay} emptyText="暂无可回放事件"/>
                            ) : (
                                <EventTimeline items={inspection.events} emptyText="暂无运行事件"/>
                            )}
                        </section>
                    )}
                </>
            )}

            <RecordDialog
                open={dialog.type === 'connection'}
                title={dialog.row ? '编辑连接凭据' : '新增连接凭据'}
                description="公开配置用于连接参数，敏感配置由后端加密保存。"
                fields={CONNECTION_FIELDS}
                initialValues={dialog.row
                    ? {...dialog.row, secrets: secretTemplate(dialog.row)}
                    : {status: 'ACTIVE', config: {}, secrets: {}}}
                submitLabel="保存凭据"
                busy={actionBusy === `connection-${dialog.row?.id || 'create'}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={saveConnection}
            />

            <RecordDialog
                open={dialog.type === 'run'}
                title={`运行流程：${selectedFlow?.name || ''}`}
                description="运行变量将作为 Flow DSL 的初始输入。"
                fields={RUN_FIELDS}
                initialValues={{variables: {}}}
                submitLabel="开始运行"
                busy={actionBusy === `run-${selectedFlowId}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={startRun}
            />

            <RecordDialog
                open={dialog.type === 'cancel'}
                title="取消流程运行"
                description={dialog.row ? `运行编号：${dialog.row.runNo || dialog.row.id}` : ''}
                fields={CANCEL_FIELDS}
                initialValues={{reason: '手动取消'}}
                submitLabel="确认取消"
                busy={actionBusy === `cancel-${dialog.row?.id}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={cancelRun}
            />
        </div>
    )
}
