import {useCallback, useEffect, useMemo, useRef, useState} from 'react'
import {reportApi} from '../../api'
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
import './ReportWorkspacePage.css'

const REPORT_VIEWS = [
    {key: 'datasets', label: '数据集'},
    {key: 'templates', label: '报表模板'}
]

const STATUS_OPTIONS = [
    {value: 'ACTIVE', label: '启用'},
    {value: 'DISABLED', label: '停用'}
]

const DATASET_FIELDS = [
    {name: 'name', label: '数据集名称', required: true, placeholder: '例如：订单日报'},
    {name: 'datasetKey', label: '数据集标识', required: true, placeholder: '例如：daily_orders'},
    {
        name: 'querySql',
        label: '查询 SQL',
        type: 'textarea',
        required: true,
        fullWidth: true,
        placeholder: 'SELECT ... WHERE created_at >= :startTime',
        hint: '仅支持只读 SELECT；参数名称与下方默认参数 JSON 保持一致。'
    },
    {
        name: 'params',
        label: '默认参数 JSON',
        type: 'json',
        fullWidth: true,
        defaultValue: '{}',
        placeholder: '{\n  "startTime": "2026-01-01"\n}'
    },
    {name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS, defaultValue: 'ACTIVE'}
]

const QUERY_FIELDS = [
    {
        name: 'params',
        label: '查询参数 JSON',
        type: 'json',
        fullWidth: true,
        defaultValue: '{}',
        placeholder: '{\n  "startTime": "2026-01-01"\n}',
        hint: '参数会作为命名参数传给数据集 SQL。留空时使用空对象。'
    },
    {
        name: 'limit',
        label: '最大返回行数',
        type: 'number',
        defaultValue: 500,
        hint: '允许 1 至 5000 行。'
    }
]

function normalizeView(view) {
    return REPORT_VIEWS.some((item) => item.key === view) ? view : 'datasets'
}

function jsonText(value, fallback = '{}') {
    if (value === undefined || value === null || value === '') return fallback
    if (typeof value === 'string') return value
    try {
        return JSON.stringify(value, null, 2)
    } catch {
        return fallback
    }
}

function formatDateTime(value) {
    if (!value) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    return new Intl.DateTimeFormat('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    }).format(date)
}

function normalizeLimit(value) {
    const number = Number(value)
    if (!Number.isFinite(number)) return 500
    return Math.min(5000, Math.max(1, Math.trunc(number)))
}

function parseJsonObjectField(value, fallback = {}) {
    const parsed = parseJsonField(value, fallback)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        throw new Error('报表参数必须是 JSON 对象')
    }
    return parsed
}

function safeFileName(value) {
    return String(value || 'report')
        .trim()
        .replace(/[<>:"/\\|?*\u0000-\u001f]/g, '-')
        .replace(/\s+/g, '-')
        .slice(0, 80) || 'report'
}

async function toCsvBlob(result) {
    if (result instanceof Blob) return result
    if (result instanceof ArrayBuffer) return new Blob([result], {type: 'text/csv;charset=utf-8'})
    if (result?.blob instanceof Blob) return result.blob
    if (typeof result?.blob === 'function') return result.blob()
    if (typeof result === 'string') return new Blob([result], {type: 'text/csv;charset=utf-8'})
    return new Blob([JSON.stringify(result ?? '')], {type: 'text/csv;charset=utf-8'})
}

function collectDynamicColumns(rows) {
    const keys = []
    const seen = new Set()
    rows.forEach((row) => {
        if (!row || typeof row !== 'object' || Array.isArray(row)) return
        Object.keys(row).forEach((key) => {
            if (!seen.has(key)) {
                seen.add(key)
                keys.push(key)
            }
        })
    })
    return keys.map((key) => ({key, label: key}))
}

function reportErrorMessage(error, fallback) {
    return error?.message || fallback
}

export default function ReportWorkspacePage({initialView = 'datasets', permissions = [], onError}) {
    const [activeView, setActiveView] = useState(() => normalizeView(initialView))
    const [datasets, setDatasets] = useState([])
    const [templates, setTemplates] = useState([])
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState('')
    const [error, setError] = useState('')
    const [recordDialog, setRecordDialog] = useState(null)
    const [queryDialog, setQueryDialog] = useState(null)
    const [queryResult, setQueryResult] = useState(null)
    const loadVersionRef = useRef(0)
    const mountedRef = useRef(true)
    const canEdit = hasPermission(permissions, 'biz:report:edit')
    const canQuery = hasPermission(permissions, 'biz:report:query')
    const canExport = hasPermission(permissions, 'biz:report:export')

    useEffect(() => {
        setActiveView(normalizeView(initialView))
    }, [initialView])

    useEffect(() => {
        mountedRef.current = true
        return () => {
            mountedRef.current = false
        }
    }, [])

    const notifyError = useCallback((message) => {
        setError(message)
        onError?.(message)
    }, [onError])

    const loadWorkspace = useCallback(async () => {
        const version = loadVersionRef.current + 1
        loadVersionRef.current = version
        setLoading(true)
        setError('')
        try {
            const [datasetRows, templateRows] = await Promise.all([
                reportApi.datasets(),
                reportApi.templates()
            ])
            if (!mountedRef.current || version !== loadVersionRef.current) return
            setDatasets(Array.isArray(datasetRows) ? datasetRows : [])
            setTemplates(Array.isArray(templateRows) ? templateRows : [])
        } catch (loadError) {
            if (!mountedRef.current || version !== loadVersionRef.current) return
            notifyError(reportErrorMessage(loadError, '报表数据加载失败'))
        } finally {
            if (mountedRef.current && version === loadVersionRef.current) setLoading(false)
        }
    }, [notifyError])

    useEffect(() => {
        loadWorkspace()
    }, [loadWorkspace])

    const datasetById = useMemo(() => {
        return new Map(datasets.map((dataset) => [String(dataset.id), dataset]))
    }, [datasets])

    const templateFields = useMemo(() => [
        {
            name: 'datasetId',
            label: '所属数据集',
            type: 'select',
            required: true,
            options: datasets.length > 0
                ? datasets.map((dataset) => ({
                    value: String(dataset.id),
                    label: `${dataset.name || dataset.datasetKey || dataset.id} (${dataset.datasetKey || dataset.id})`
                }))
                : [{value: '', label: '请先创建数据集'}]
        },
        {name: 'name', label: '模板名称', required: true, placeholder: '例如：订单日报表'},
        {name: 'templateKey', label: '模板标识', required: true, placeholder: '例如：daily_order_report'},
        {
            name: 'template',
            label: '模板配置 JSON',
            type: 'json',
            fullWidth: true,
            required: true,
            defaultValue: '{}',
            placeholder: '{\n  "title": "订单日报",\n  "columns": []\n}'
        },
        {name: 'status', label: '状态', type: 'select', options: STATUS_OPTIONS, defaultValue: 'ACTIVE'}
    ], [datasets])

    const recordFields = recordDialog?.type === 'template' ? templateFields : DATASET_FIELDS

    const recordInitialValues = useMemo(() => {
        const row = recordDialog?.row
        if (recordDialog?.type === 'template') {
            return {
                datasetId: row?.datasetId ? String(row.datasetId) : (datasets[0]?.id ? String(datasets[0].id) : ''),
                name: row?.name || '',
                templateKey: row?.templateKey || '',
                template: jsonText(row?.template),
                status: row?.status || 'ACTIVE'
            }
        }
        return {
            name: row?.name || '',
            datasetKey: row?.datasetKey || '',
            querySql: row?.querySql || '',
            params: jsonText(row?.params),
            status: row?.status || 'ACTIVE'
        }
    }, [datasets, recordDialog])

    const queryInitialValues = useMemo(() => ({
        params: jsonText(queryDialog?.dataset?.params),
        limit: 500
    }), [queryDialog])

    const datasetColumns = useMemo(() => [
        {key: 'name', label: '数据集名称'},
        {key: 'datasetKey', label: '数据集标识', render: (value) => <code>{value || '—'}</code>},
        {
            key: 'querySql',
            label: '查询 SQL',
            render: (value) => <code className="report-sql-cell">{value || '—'}</code>
        },
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'updateTime', label: '更新时间', render: (value) => formatDateTime(value)}
    ], [])

    const templateColumns = useMemo(() => [
        {key: 'name', label: '模板名称'},
        {key: 'templateKey', label: '模板标识', render: (value) => <code>{value || '—'}</code>},
        {
            key: 'datasetId',
            label: '所属数据集',
            value: (row) => datasetById.get(String(row.datasetId))?.name || row.datasetId,
            render: (value, row) => {
                const dataset = datasetById.get(String(row.datasetId))
                return dataset ? (
                    <span className="report-dataset-reference">
              <strong>{dataset.name}</strong>
              <small>{dataset.datasetKey}</small>
            </span>
                ) : (value || '—')
            }
        },
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'updateTime', label: '更新时间', render: (value) => formatDateTime(value)}
    ], [datasetById])

    const dynamicColumns = useMemo(
        () => collectDynamicColumns(queryResult?.rows || []),
        [queryResult]
    )

    const metrics = useMemo(() => {
        const activeDatasets = datasets.filter((item) => String(item.status).toUpperCase() === 'ACTIVE').length
        const activeTemplates = templates.filter((item) => String(item.status).toUpperCase() === 'ACTIVE').length
        return [
            {label: '数据集', value: datasets.length, hint: `${activeDatasets} 个启用`},
            {label: '报表模板', value: templates.length, hint: `${activeTemplates} 个启用`},
            {
                label: '已关联模板',
                value: templates.filter((item) => datasetById.has(String(item.datasetId))).length,
                hint: '关联现有数据集'
            },
            {
                label: '最近查询',
                value: queryResult ? `${queryResult.rows.length} 行` : '未执行',
                hint: queryResult ? queryResult.dataset.name : '选择数据集后查询'
            }
        ]
    }, [datasetById, datasets, queryResult, templates])

    function openCreateDialog() {
        if (activeView === 'templates' && datasets.length === 0) {
            notifyError('请先创建数据集，再新增报表模板')
            return
        }
        setError('')
        setRecordDialog({type: activeView === 'templates' ? 'template' : 'dataset', row: null})
    }

    function openEditDialog(type, row) {
        setError('')
        setRecordDialog({type, row})
    }

    async function submitRecord(values) {
        const type = recordDialog?.type
        const row = recordDialog?.row
        setBusy(`save-${type}`)
        setError('')
        try {
            if (type === 'template') {
                await reportApi.saveTemplate({
                    ...(row?.id ? {id: row.id} : {}),
                    datasetId: values.datasetId,
                    name: values.name?.trim(),
                    templateKey: values.templateKey?.trim(),
                    template: parseJsonField(values.template, {}),
                    status: values.status || 'ACTIVE'
                })
            } else {
                await reportApi.saveDataset({
                    ...(row?.id ? {id: row.id} : {}),
                    name: values.name?.trim(),
                    datasetKey: values.datasetKey?.trim(),
                    querySql: values.querySql?.trim(),
                    params: parseJsonObjectField(values.params),
                    status: values.status || 'ACTIVE'
                })
            }
            setRecordDialog(null)
            await loadWorkspace()
        } catch (saveError) {
            notifyError(reportErrorMessage(saveError, '报表配置保存失败'))
            throw saveError
        } finally {
            if (mountedRef.current) setBusy('')
        }
    }

    function openDatasetAction(action, dataset) {
        const currentDataset = datasetById.get(String(dataset?.id))
        if (!currentDataset) {
            notifyError('数据集已不存在，请刷新列表后重试')
            return
        }
        if (String(currentDataset.status).toUpperCase() === 'DISABLED') {
            notifyError('已停用的数据集不能查询或导出')
            return
        }
        setError('')
        setQueryDialog({action, dataset: currentDataset})
    }

    async function submitDatasetAction(values) {
        const dataset = queryDialog?.dataset
        const action = queryDialog?.action
        if (!dataset?.id || !action) return
        setBusy(`${action}-dataset`)
        setError('')
        try {
            const params = parseJsonObjectField(values.params)
            const limit = normalizeLimit(values.limit)
            if (action === 'export') {
                const result = await reportApi.exportDataset(dataset.id, params, limit)
                const blob = await toCsvBlob(result)
                const url = window.URL.createObjectURL(blob)
                const link = document.createElement('a')
                link.href = url
                link.download = `${safeFileName(dataset.datasetKey || dataset.name)}-${Date.now()}.csv`
                link.click()
                window.setTimeout(() => window.URL.revokeObjectURL(url), 0)
            } else {
                const rows = await reportApi.queryDataset(dataset.id, params, limit)
                setQueryResult({
                    dataset,
                    rows: Array.isArray(rows) ? rows : [],
                    params,
                    limit,
                    completedAt: new Date()
                })
            }
            setQueryDialog(null)
        } catch (actionError) {
            notifyError(reportErrorMessage(
                actionError,
                action === 'export' ? 'CSV 导出失败' : '数据集查询失败'
            ))
            throw actionError
        } finally {
            if (mountedRef.current) setBusy('')
        }
    }

    const headerActionLabel = activeView === 'templates' ? '新增模板' : '新增数据集'
    const activeRows = activeView === 'templates' ? templates : datasets
    const activeColumns = activeView === 'templates' ? templateColumns : datasetColumns

    return (
        <section className="report-workspace-page" aria-labelledby="report-workspace-title">
            <WorkspaceHeader
                breadcrumb="业务应用 / 报表中心"
                title="报表工作区"
                description="维护可复用的数据集和报表模板，并直接验证参数化查询或导出 CSV。"
                status={error ? 'ERROR' : loading ? 'RUNNING' : 'ACTIVE'}
                loading={loading}
                onRefresh={loadWorkspace}
                actions={canEdit ? (
                    <button
                        type="button"
                        className="button primary"
                        disabled={Boolean(busy) || (activeView === 'templates' && datasets.length === 0)}
                        onClick={openCreateDialog}
                    >
                        {headerActionLabel}
                    </button>
                ) : null}
            />

            {error && (
                <div className="report-workspace-error" role="alert">
                    <span>{error}</span>
                    <button type="button" className="text-button" onClick={() => setError('')}>关闭</button>
                </div>
            )}

            <WorkspaceTabs
                items={REPORT_VIEWS.map((view) => ({
                    ...view,
                    count: view.key === 'datasets' ? datasets.length : templates.length
                }))}
                activeKey={activeView}
                onChange={setActiveView}
                label="报表资源"
            />

            <MetricStrip items={metrics}/>

            <BackendDataTable
                ariaLabel={activeView === 'templates' ? '报表模板列表' : '数据集列表'}
                columns={activeColumns}
                rows={activeRows}
                loading={loading}
                emptyText={activeView === 'templates' ? '暂无报表模板' : '暂无数据集'}
                searchPlaceholder={activeView === 'templates' ? '搜索模板名称、标识或数据集' : '搜索数据集、SQL 或状态'}
                rowActions={activeView === 'templates'
                    ? (canEdit ? (row) => (
                        <button type="button" className="table-link" onClick={() => openEditDialog('template', row)}>
                            编辑
                        </button>
                    ) : undefined)
                    : (canQuery || canExport || canEdit ? (row) => (
                        <>
                            {canQuery && (
                                <button
                                    type="button"
                                    className="table-link"
                                    disabled={Boolean(busy) || String(row.status).toUpperCase() === 'DISABLED'}
                                    title={String(row.status).toUpperCase() === 'DISABLED' ? '已停用的数据集不能查询' : undefined}
                                    onClick={() => openDatasetAction('query', row)}
                                >
                                    查询
                                </button>
                            )}
                            {canExport && (
                                <button
                                    type="button"
                                    className="table-link"
                                    disabled={Boolean(busy) || String(row.status).toUpperCase() === 'DISABLED'}
                                    title={String(row.status).toUpperCase() === 'DISABLED' ? '已停用的数据集不能导出' : undefined}
                                    onClick={() => openDatasetAction('export', row)}
                                >
                                    导出 CSV
                                </button>
                            )}
                            {canEdit && (
                                <button type="button" className="table-link"
                                        onClick={() => openEditDialog('dataset', row)}>
                                    编辑
                                </button>
                            )}
                        </>
                    ) : undefined)}
            />

            {queryResult && (
                <section className="report-query-result" aria-labelledby="report-query-result-title">
                    <header>
                        <div>
                            <p className="page-breadcrumb">查询结果</p>
                            <h2 id="report-query-result-title">{queryResult.dataset.name}</h2>
                            <p>
                                返回 {queryResult.rows.length} 行，限制 {queryResult.limit} 行，
                                完成于 {formatDateTime(queryResult.completedAt)}
                            </p>
                        </div>
                        <div className="report-query-result-actions">
                            {canQuery && (
                                <button
                                    type="button"
                                    className="button ghost"
                                    disabled={Boolean(busy)}
                                    onClick={() => openDatasetAction('query', queryResult.dataset)}
                                >
                                    重新查询
                                </button>
                            )}
                            <button type="button" className="text-button" onClick={() => setQueryResult(null)}>
                                关闭结果
                            </button>
                        </div>
                    </header>
                    <BackendDataTable
                        ariaLabel={`${queryResult.dataset.name}查询结果`}
                        columns={dynamicColumns}
                        rows={queryResult.rows}
                        emptyText="查询成功，但没有返回记录"
                        searchPlaceholder="搜索查询结果"
                        rowKey="__reportRowKey"
                    />
                </section>
            )}

            <RecordDialog
                open={Boolean(recordDialog)}
                title={`${recordDialog?.row ? '编辑' : '新增'}${recordDialog?.type === 'template' ? '报表模板' : '数据集'}`}
                description={recordDialog?.type === 'template'
                    ? '模板通过数据集取得数据，JSON 配置可保存展示字段、标题和布局信息。'
                    : '数据集 SQL 仅允许只读查询；保存后可在列表中使用参数进行即时验证。'}
                fields={recordFields}
                initialValues={recordInitialValues}
                busy={busy === `save-${recordDialog?.type}`}
                onClose={() => setRecordDialog(null)}
                onSubmit={submitRecord}
            />

            <RecordDialog
                open={Boolean(queryDialog)}
                title={`${queryDialog?.action === 'export' ? '导出' : '查询'}数据集：${queryDialog?.dataset?.name || ''}`}
                description={queryDialog?.action === 'export'
                    ? '使用以下参数执行只读查询，并将结果下载为 UTF-8 CSV 文件。'
                    : '使用以下参数执行只读查询，动态结果会显示在数据集列表下方。'}
                fields={QUERY_FIELDS}
                initialValues={queryInitialValues}
                submitLabel={queryDialog?.action === 'export' ? '导出 CSV' : '执行查询'}
                busy={busy === `${queryDialog?.action}-dataset`}
                onClose={() => setQueryDialog(null)}
                onSubmit={submitDatasetAction}
            />
        </section>
    )
}
