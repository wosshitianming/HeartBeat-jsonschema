import {useCallback, useEffect, useMemo, useState} from 'react'
import {workflowApi} from '../../api'
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

const TAB_ITEMS = [
    {key: 'definitions', label: '流程定义'},
    {key: 'instances', label: '流程实例'},
    {key: 'tasks', label: '我的待办'}
]

const DEFINITION_FIELDS = [
    {name: 'name', label: '流程名称', required: true},
    {name: 'definitionKey', label: '流程标识', required: true, placeholder: '例如 expense_approval'},
    {name: 'versionNo', label: '版本号', type: 'number', defaultValue: 1},
    {name: 'formSchema', label: '表单 Schema', type: 'json', fullWidth: true, defaultValue: {}},
    {name: 'bpmnXml', label: 'BPMN XML', type: 'textarea', fullWidth: true, placeholder: '可粘贴 BPMN 2.0 XML'}
]

const START_FIELDS = [
    {name: 'businessKey', label: '业务单号', required: true},
    {name: 'title', label: '流程标题', required: true},
    {name: 'assigneeId', label: '办理人 ID'},
    {name: 'approverId', label: '审批人 ID'},
    {name: 'payload', label: '业务数据', type: 'json', fullWidth: true, defaultValue: {}}
]

const ACTION_FIELDS = [
    {name: 'comment', label: '处理意见', type: 'textarea', fullWidth: true, placeholder: '填写审批或驳回意见'}
]

function withTabCounts(items, definitions, instances, tasks) {
    const counts = {definitions: definitions.length, instances: instances.length, tasks: tasks.length}
    return items.map((item) => ({...item, count: counts[item.key]}))
}

function normalizeView(view, canViewWorkflow, canViewTodo) {
    if (view === 'tasks' && canViewTodo) return 'tasks'
    if ((view === 'definitions' || view === 'instances') && canViewWorkflow) return view
    if (canViewWorkflow) return 'definitions'
    if (canViewTodo) return 'tasks'
    return 'definitions'
}

export default function WorkflowWorkspacePage({initialView = 'definitions', currentUser, permissions = [], onError}) {
    const canViewWorkflow = hasPermission(permissions, 'biz:workflow:list')
    const canViewTodo = hasPermission(permissions, 'biz:workflow:todo')
    const [activeView, setActiveView] = useState(() => normalizeView(initialView, canViewWorkflow, canViewTodo))
    const [definitions, setDefinitions] = useState([])
    const [instances, setInstances] = useState([])
    const [tasks, setTasks] = useState([])
    const [loading, setLoading] = useState(true)
    const [actionBusy, setActionBusy] = useState('')
    const [localError, setLocalError] = useState('')
    const [dialog, setDialog] = useState({type: '', row: null})
    const canEdit = hasPermission(permissions, 'biz:workflow:edit')
    const canDeploy = hasPermission(permissions, 'biz:workflow:deploy')
    const canStart = hasPermission(permissions, 'biz:workflow:start')
    const canApprove = hasPermission(permissions, 'biz:workflow:approve')

    useEffect(() => {
        setActiveView(normalizeView(initialView, canViewWorkflow, canViewTodo))
    }, [canViewTodo, canViewWorkflow, initialView])

    const reportError = useCallback((error) => {
        const message = error?.message || '工作流操作失败'
        setLocalError(message)
        onError?.(message)
    }, [onError])

    const loadAll = useCallback(async () => {
        setLoading(true)
        setLocalError('')
        const results = await Promise.allSettled([
            canViewWorkflow ? workflowApi.definitions() : Promise.resolve([]),
            canViewWorkflow ? workflowApi.instances() : Promise.resolve([]),
            canViewTodo ? workflowApi.todoTasks() : Promise.resolve([])
        ])
        const [definitionResult, instanceResult, taskResult] = results
        if (definitionResult.status === 'fulfilled') setDefinitions(definitionResult.value || [])
        if (instanceResult.status === 'fulfilled') setInstances(instanceResult.value || [])
        if (taskResult.status === 'fulfilled') setTasks(taskResult.value || [])
        const rejected = results.find((item) => item.status === 'rejected')
        if (rejected) reportError(rejected.reason)
        setLoading(false)
    }, [canViewTodo, canViewWorkflow, reportError])

    useEffect(() => {
        loadAll()
    }, [loadAll])

    const tabs = useMemo(
        () => withTabCounts(
            TAB_ITEMS.filter((item) => (
                item.key === 'tasks' ? canViewTodo : canViewWorkflow
            )),
            definitions,
            instances,
            tasks
        ),
        [canViewTodo, canViewWorkflow, definitions, instances, tasks]
    )
    const runningCount = instances.filter((item) => ['RUNNING', 'PENDING', 'PROCESSING'].includes(item.status)).length
    const deployedCount = definitions.filter((item) => item.status === 'DEPLOYED').length

    async function execute(key, action, rethrow = false) {
        setActionBusy(key)
        setLocalError('')
        try {
            await action()
            await loadAll()
            setDialog({type: '', row: null})
        } catch (error) {
            reportError(error)
            if (rethrow) throw error
        } finally {
            setActionBusy('')
        }
    }

    async function createDefinition(values) {
        await execute('create-definition', () => workflowApi.createDefinition({
            ...values,
            versionNo: Number(values.versionNo || 1),
            formSchema: parseJsonField(values.formSchema, {})
        }), true)
    }

    async function startInstance(values) {
        if (!currentUser?.id) throw new Error('无法识别当前登录账号，请重新登录后再发起流程')
        await execute(`start-${dialog.row?.id}`, () => workflowApi.startInstance(dialog.row.id, {
            ...values,
            initiatorId: String(currentUser.id),
            payload: parseJsonField(values.payload, {})
        }), true)
    }

    async function submitTaskAction(values) {
        const action = dialog.type === 'approve' ? workflowApi.approveTask : workflowApi.rejectTask
        await execute(`${dialog.type}-${dialog.row?.id}`, () => action(dialog.row.id, values), true)
    }

    const definitionColumns = [
        {key: 'name', label: '流程名称'},
        {key: 'definitionKey', label: '流程标识'},
        {key: 'versionNo', label: '版本'},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'deployedAt', label: '部署时间'}
    ]
    const instanceColumns = [
        {key: 'title', label: '流程标题'},
        {key: 'businessKey', label: '业务单号'},
        {key: 'definitionId', label: '定义 ID'},
        {key: 'initiatorId', label: '发起人'},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'startedAt', label: '发起时间'}
    ]
    const taskColumns = [
        {key: 'name', label: '任务名称'},
        {key: 'instanceId', label: '流程实例'},
        {key: 'assigneeId', label: '办理人'},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'createTime', label: '到达时间'}
    ]

    return (
        <div className="hb-page-card backend-workspace-page">
            <WorkspaceHeader
                breadcrumb="业务中心 / 审批中心"
                title="工作流审批"
                description="统一管理流程定义、运行实例和当前账号的待办任务，部署与审批动作直接调用后端工作流服务。"
                status="已接入"
                loading={loading}
                onRefresh={loadAll}
                actions={activeView === 'definitions' && canEdit ? (
                    <button className="button primary" type="button"
                            onClick={() => setDialog({type: 'definition', row: null})}>
                        新建流程
                    </button>
                ) : null}
            />

            {localError && <div className="error-banner" role="alert">{localError}</div>}

            <WorkspaceTabs items={tabs} activeKey={activeView} onChange={setActiveView}/>
            <MetricStrip items={[
                {label: '流程定义', value: definitions.length, hint: `${deployedCount} 个已部署`},
                {label: '运行实例', value: instances.length, hint: `${runningCount} 个处理中`},
                {label: '当前待办', value: tasks.length, hint: '按当前登录账号过滤'},
                {label: '服务状态', value: loading ? '同步中' : '可用', hint: 'Workflow API'}
            ]}/>

            {activeView === 'definitions' && (
                <BackendDataTable
                    ariaLabel="流程定义列表"
                    columns={definitionColumns}
                    rows={definitions}
                    loading={loading}
                    emptyText="暂无流程定义，可先新建一个审批流程"
                    searchPlaceholder="搜索流程名称或标识"
                    rowActions={canDeploy || canStart ? (row) => (
                        <>
                            {canDeploy && row.status !== 'DEPLOYED' && (
                                <button
                                    className="table-link"
                                    type="button"
                                    disabled={Boolean(actionBusy)}
                                    onClick={() => execute(`deploy-${row.id}`, () => workflowApi.deployDefinition(row.id))}
                                >
                                    部署
                                </button>
                            )}
                            {canStart && row.status === 'DEPLOYED' && (
                                <button
                                    className="table-link"
                                    type="button"
                                    disabled={Boolean(actionBusy)}
                                    onClick={() => setDialog({type: 'start', row})}
                                >
                                    发起
                                </button>
                            )}
                        </>
                    ) : undefined}
                />
            )}

            {activeView === 'instances' && (
                <BackendDataTable
                    ariaLabel="流程实例列表"
                    columns={instanceColumns}
                    rows={instances}
                    loading={loading}
                    emptyText="暂无运行中的流程实例"
                    searchPlaceholder="搜索标题或业务单号"
                />
            )}

            {activeView === 'tasks' && (
                <BackendDataTable
                    ariaLabel="待办任务列表"
                    columns={taskColumns}
                    rows={tasks}
                    loading={loading}
                    emptyText="当前账号没有待办任务"
                    searchPlaceholder="搜索任务或实例 ID"
                    rowActions={canApprove ? (row) => (
                        <>
                            <button
                                className="table-link"
                                type="button"
                                disabled={Boolean(actionBusy)}
                                onClick={() => setDialog({type: 'approve', row})}
                            >
                                通过
                            </button>
                            <button
                                className="table-link danger-text"
                                type="button"
                                disabled={Boolean(actionBusy)}
                                onClick={() => setDialog({type: 'reject', row})}
                            >
                                驳回
                            </button>
                        </>
                    ) : undefined}
                />
            )}

            <RecordDialog
                open={dialog.type === 'definition'}
                title="新建流程定义"
                description="保存后可在列表中部署，并直接发起业务流程。"
                fields={DEFINITION_FIELDS}
                initialValues={{versionNo: 1, formSchema: {type: 'object', properties: {}}}}
                submitLabel="保存定义"
                busy={actionBusy === 'create-definition'}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={createDefinition}
            />

            <RecordDialog
                open={dialog.type === 'start'}
                title={`发起流程：${dialog.row?.name || ''}`}
                description="发起人固定为当前登录账号；业务单号用于关联外部数据，办理人与审批人可按实际组织账号填写。"
                fields={START_FIELDS}
                initialValues={{
                    businessKey: `WF-${Date.now()}`,
                    title: dialog.row?.name || '',
                    payload: {}
                }}
                submitLabel="确认发起"
                busy={actionBusy === `start-${dialog.row?.id}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={startInstance}
            />

            <RecordDialog
                open={dialog.type === 'approve' || dialog.type === 'reject'}
                title={dialog.type === 'approve' ? '通过待办任务' : '驳回待办任务'}
                description={dialog.row ? `${dialog.row.name || '当前任务'} · 实例 ${dialog.row.instanceId || '—'}` : ''}
                fields={ACTION_FIELDS}
                initialValues={{comment: ''}}
                submitLabel={dialog.type === 'approve' ? '确认通过' : '确认驳回'}
                busy={actionBusy === `${dialog.type}-${dialog.row?.id}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={submitTaskAction}
            />
        </div>
    )
}
