import {useCallback, useEffect, useMemo, useState} from 'react'
import {mpApi} from '../../api'
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
import './MpWorkspacePage.css'

const VIEW_ITEMS = [
    {key: 'accounts', label: '公众号账号'},
    {key: 'menus', label: '菜单配置'},
    {key: 'materials', label: '素材管理'},
    {key: 'replies', label: '自动回复'}
]

const ACCOUNT_FIELDS = [
    {name: 'name', label: '账号名称', required: true},
    {name: 'appId', label: 'App ID', required: true},
    {name: 'appSecret', label: 'App Secret', type: 'password', hint: '编辑时留空将保留现有密钥'},
    {
        name: 'token',
        label: '菜单同步 Access Token',
        type: 'password',
        hint: '现有后端同步器直接使用此字段调用微信接口；编辑时留空将保留现有值。'
    },
    {name: 'aesKey', label: 'EncodingAESKey', type: 'password', hint: '编辑时留空将保留现有密钥'},
    {name: 'status', label: '状态', type: 'select', options: ['ACTIVE', 'DISABLED'], defaultValue: 'ACTIVE'}
]

const MATERIAL_FIELDS = [
    {
        name: 'materialType',
        label: '素材类型',
        type: 'select',
        options: ['text', 'image', 'voice', 'video', 'news'],
        defaultValue: 'text'
    },
    {name: 'title', label: '素材标题', required: true},
    {name: 'mediaId', label: '媒体 ID'},
    {name: 'url', label: '素材地址'},
    {name: 'status', label: '状态', type: 'select', options: ['ACTIVE', 'DISABLED'], defaultValue: 'ACTIVE'},
    {name: 'payload', label: '扩展内容', type: 'json', fullWidth: true, defaultValue: {}}
]

const REPLY_FIELDS = [
    {name: 'keyword', label: '匹配关键词', required: true},
    {name: 'matchType', label: '匹配方式', type: 'select', options: ['EXACT', 'CONTAINS'], defaultValue: 'EXACT'},
    {name: 'replyType', label: '回复类型', type: 'select', options: ['TEXT', 'IMAGE', 'NEWS'], defaultValue: 'TEXT'},
    {name: 'sortNo', label: '排序', type: 'number', defaultValue: 0},
    {name: 'status', label: '状态', type: 'select', options: ['ACTIVE', 'DISABLED'], defaultValue: 'ACTIVE'},
    {name: 'replyContent', label: '回复内容', type: 'json', fullWidth: true, defaultValue: {text: ''}}
]

const ACCOUNT_COLUMNS = [
    {key: 'name', label: '账号名称'},
    {key: 'appId', label: 'App ID'},
    {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
    {key: 'createTime', label: '创建时间'},
    {key: 'updateTime', label: '更新时间'}
]

const MENU_COLUMNS = [
    {key: 'name', label: '菜单名称'},
    {key: 'menuType', label: '类型'},
    {key: 'parentId', label: '上级 ID'},
    {key: 'url', label: '跳转地址'},
    {key: 'sortNo', label: '排序'},
    {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>}
]

const MATERIAL_COLUMNS = [
    {key: 'title', label: '素材标题'},
    {key: 'materialType', label: '类型'},
    {key: 'mediaId', label: '媒体 ID'},
    {key: 'url', label: '素材地址'},
    {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
    {key: 'updateTime', label: '更新时间'}
]

const REPLY_COLUMNS = [
    {key: 'keyword', label: '关键词'},
    {key: 'matchType', label: '匹配方式'},
    {key: 'replyType', label: '回复类型'},
    {key: 'sortNo', label: '排序'},
    {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
    {key: 'updateTime', label: '更新时间'}
]

function normalizeView(view) {
    if (view === 'auto-replies') return 'replies'
    return VIEW_ITEMS.some((item) => item.key === view) ? view : 'accounts'
}

function sanitizeAccount(account) {
    if (!account || typeof account !== 'object' || Array.isArray(account)) return account
    const safeAccount = {...account}
    delete safeAccount.appSecret
    delete safeAccount.token
    delete safeAccount.aesKey
    return safeAccount
}

export default function MpWorkspacePage({initialView = 'accounts', permissions = [], onError}) {
    const [activeView, setActiveView] = useState(() => normalizeView(initialView))
    const [accounts, setAccounts] = useState([])
    const [selectedAccountId, setSelectedAccountId] = useState('')
    const [menus, setMenus] = useState([])
    const [materials, setMaterials] = useState([])
    const [replies, setReplies] = useState([])
    const [accountsLoading, setAccountsLoading] = useState(true)
    const [childrenLoading, setChildrenLoading] = useState(false)
    const [actionBusy, setActionBusy] = useState('')
    const [localError, setLocalError] = useState('')
    const [feedback, setFeedback] = useState('')
    const [dialog, setDialog] = useState({type: '', row: null})
    const canEdit = hasPermission(permissions, 'biz:mp:edit')
    const canSync = hasPermission(permissions, 'biz:mp:sync')

    useEffect(() => setActiveView(normalizeView(initialView)), [initialView])

    const reportError = useCallback((error) => {
        if (error?.name === 'AbortError') return
        const message = error?.message || '公众号操作失败'
        setLocalError(message)
        onError?.(message)
    }, [onError])

    const fetchAccounts = useCallback(async (preferredId, options = {}) => {
        const items = await mpApi.accounts(options)
        const safeItems = (Array.isArray(items) ? items : []).map(sanitizeAccount)
        setAccounts(safeItems)
        setSelectedAccountId((current) => {
            const target = preferredId == null ? current : String(preferredId)
            if (target && safeItems.some((item) => String(item.id) === target)) return target
            return safeItems[0]?.id == null ? '' : String(safeItems[0].id)
        })
        return safeItems
    }, [])

    const fetchChildren = useCallback(async (accountId, options = {}) => {
        if (!accountId) {
            setMenus([])
            setMaterials([])
            setReplies([])
            return
        }
        const results = await Promise.allSettled([
            mpApi.menus(accountId, options),
            mpApi.materials(accountId, options),
            mpApi.autoReplies(accountId, options)
        ])
        if (options.signal?.aborted) return
        if (results[0].status === 'fulfilled') setMenus(results[0].value || [])
        if (results[1].status === 'fulfilled') setMaterials(results[1].value || [])
        if (results[2].status === 'fulfilled') setReplies(results[2].value || [])
        const rejected = results.find((result) => result.status === 'rejected' && result.reason?.name !== 'AbortError')
        if (rejected) throw rejected.reason
    }, [])

    useEffect(() => {
        const controller = new AbortController()
        setAccountsLoading(true)
        fetchAccounts(undefined, {signal: controller.signal})
            .catch(reportError)
            .finally(() => {
                if (!controller.signal.aborted) setAccountsLoading(false)
            })
        return () => controller.abort()
    }, [fetchAccounts, reportError])

    useEffect(() => {
        if (!selectedAccountId) {
            setMenus([])
            setMaterials([])
            setReplies([])
            return undefined
        }
        const controller = new AbortController()
        setChildrenLoading(true)
        fetchChildren(selectedAccountId, {signal: controller.signal})
            .catch(reportError)
            .finally(() => {
                if (!controller.signal.aborted) setChildrenLoading(false)
            })
        return () => controller.abort()
    }, [fetchChildren, reportError, selectedAccountId])

    const selectedAccount = useMemo(
        () => accounts.find((item) => String(item.id) === selectedAccountId),
        [accounts, selectedAccountId]
    )

    const tabs = useMemo(() => {
        const counts = {
            accounts: accounts.length,
            menus: menus.length,
            materials: materials.length,
            replies: replies.length
        }
        return VIEW_ITEMS.map((item) => ({...item, count: counts[item.key]}))
    }, [accounts.length, materials.length, menus.length, replies.length])

    const menuFields = useMemo(() => [
        {
            name: 'parentId',
            label: '上级菜单',
            type: 'select',
            options: [
                {value: '0', label: '顶级菜单'},
                ...menus
                    .filter((item) => (
                        String(item.id) !== String(dialog.row?.id || '')
                        && (!item.parentId || String(item.parentId) === '0')
                    ))
                    .map((item) => ({value: item.id, label: item.name || `菜单 ${item.id}`}))
            ]
        },
        {name: 'name', label: '菜单名称', required: true},
        {
            name: 'menuType',
            label: '菜单类型',
            type: 'select',
            options: [
                {value: 'view', label: '跳转网页'},
                {value: 'click', label: '点击事件'},
                {value: 'miniprogram', label: '小程序'}
            ],
            defaultValue: 'view'
        },
        {name: 'url', label: '跳转地址或事件 Key'},
        {name: 'sortNo', label: '排序', type: 'number', defaultValue: 0},
        {name: 'status', label: '状态', type: 'select', options: ['ACTIVE', 'DISABLED'], defaultValue: 'ACTIVE'},
        {name: 'payload', label: '扩展配置', type: 'json', fullWidth: true, defaultValue: {}}
    ], [dialog.row?.id, menus])

    const loading = accountsLoading || childrenLoading
    const activeAccounts = accounts.filter((item) => item.status === 'ACTIVE').length
    const activeMenus = menus.filter((item) => item.status === 'ACTIVE').length
    const activeReplies = replies.filter((item) => item.status === 'ACTIVE').length

    async function runAction(key, action, successMessage, after, rethrow = false) {
        setActionBusy(key)
        setLocalError('')
        setFeedback('')
        try {
            const result = await action()
            await after?.(result)
            setDialog({type: '', row: null})
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
        setAccountsLoading(true)
        setChildrenLoading(Boolean(selectedAccountId))
        setLocalError('')
        setFeedback('')
        try {
            await fetchAccounts(selectedAccountId)
            if (selectedAccountId) await fetchChildren(selectedAccountId)
        } catch (error) {
            reportError(error)
        } finally {
            setAccountsLoading(false)
            setChildrenLoading(false)
        }
    }

    async function saveAccount(values) {
        const rowId = dialog.row?.id
        await runAction(`account-${rowId || 'create'}`, () => mpApi.saveAccount({
            ...values,
            id: rowId
        }), rowId ? '公众号账号已更新' : '公众号账号已创建', async (saved) => {
            await fetchAccounts(saved?.id || rowId || selectedAccountId)
        }, true)
    }

    async function saveMenu(values) {
        await runAction(`menu-${dialog.row?.id || 'create'}`, () => mpApi.saveMenu({
            ...values,
            id: dialog.row?.id,
            accountId: selectedAccountId,
            parentId: values.parentId || '0',
            sortNo: Number(values.sortNo || 0),
            payload: parseJsonField(values.payload, {})
        }), dialog.row ? '公众号菜单已更新' : '公众号菜单已创建', () => fetchChildren(selectedAccountId), true)
    }

    async function saveMaterial(values) {
        await runAction(`material-${dialog.row?.id || 'create'}`, () => mpApi.saveMaterial({
            ...values,
            id: dialog.row?.id,
            accountId: selectedAccountId,
            payload: parseJsonField(values.payload, {})
        }), dialog.row ? '素材已更新' : '素材已创建', () => fetchChildren(selectedAccountId), true)
    }

    async function saveReply(values) {
        await runAction(`reply-${dialog.row?.id || 'create'}`, () => mpApi.saveAutoReply({
            ...values,
            id: dialog.row?.id,
            accountId: selectedAccountId,
            sortNo: Number(values.sortNo || 0),
            replyContent: parseJsonField(values.replyContent, {})
        }), dialog.row ? '自动回复已更新' : '自动回复已创建', () => fetchChildren(selectedAccountId), true)
    }

    async function syncMenu() {
        if (String(selectedAccount?.status).toUpperCase() !== 'ACTIVE') {
            reportError('已停用的公众号账号不能同步菜单')
            return
        }
        if (menus.length === 0) {
            reportError('当前账号没有可同步的公众号菜单')
            return
        }
        const hasZeroRoot = menus.some((item) => String(item.parentId) === '0')
        if (hasZeroRoot) {
            reportError('当前后端同步器无法识别 parentId=0 的顶级菜单，已阻止同步以避免覆盖线上公众号菜单')
            return
        }
        const hasSupportedRoot = menus.some((item) => item.parentId == null || String(item.parentId).trim() === '')
        if (!hasSupportedRoot) {
            reportError('未识别到可同步的顶级菜单，已阻止空菜单覆盖线上配置')
            return
        }
        await runAction('sync-menu', async () => {
            const result = await mpApi.syncMenu(selectedAccountId)
            const status = String(result?.status || '').toUpperCase()
            if (status !== 'SYNCED') {
                throw new Error(result?.message || '公众号菜单未完成同步')
            }
            if (typeof result?.providerResponse === 'string') {
                try {
                    const provider = JSON.parse(result.providerResponse)
                    if (Number(provider?.errcode || 0) !== 0) {
                        throw new Error(provider?.errmsg || `微信接口返回错误 ${provider.errcode}`)
                    }
                } catch (error) {
                    if (error instanceof SyntaxError) return result
                    throw error
                }
            }
            return result
        }, '公众号菜单已同步', () => fetchChildren(selectedAccountId))
    }

    const headerActions = activeView === 'accounts' && canEdit ? (
        <button className="button primary" type="button" onClick={() => setDialog({type: 'account', row: null})}>
            新增账号
        </button>
    ) : selectedAccount ? (
        <>
            {activeView === 'menus' && canSync && (
                <button className="button ghost" type="button" disabled={Boolean(actionBusy)} onClick={syncMenu}>
                    {actionBusy === 'sync-menu' ? '同步中...' : '同步菜单'}
                </button>
            )}
            {canEdit && activeView !== 'accounts' && (
                <button
                    className="button primary"
                    type="button"
                    onClick={() => setDialog({
                        type: activeView === 'replies' ? 'reply' : activeView.slice(0, -1),
                        row: null
                    })}
                >
                    {activeView === 'menus' ? '新增菜单' : activeView === 'materials' ? '新增素材' : '新增回复'}
                </button>
            )}
        </>
    ) : null

    const accountInitialValues = dialog.row
        ? {...dialog.row, appSecret: '', token: '', aesKey: ''}
        : {name: '', appId: '', appSecret: '', token: '', aesKey: '', status: 'ACTIVE'}

    return (
        <div className="hb-page-card backend-workspace-page mp-workspace-page">
            <WorkspaceHeader
                breadcrumb="业务中心 / 公众号"
                title="公众号运营"
                description="统一维护公众号账号及其菜单、素材和自动回复规则。"
                status={selectedAccount ? selectedAccount.status : '真实数据'}
                loading={loading}
                onRefresh={refreshAll}
                actions={headerActions}
            />

            {localError && <div className="error-banner" role="alert">{localError}</div>}
            {feedback && <div className="mp-feedback" role="status">{feedback}</div>}

            <WorkspaceTabs items={tabs} activeKey={activeView} onChange={setActiveView}/>

            <MetricStrip items={[
                {label: '公众号账号', value: accounts.length, hint: `${activeAccounts} 个启用`},
                {label: '当前菜单', value: menus.length, hint: `${activeMenus} 个启用`},
                {label: '素材数量', value: materials.length, hint: selectedAccount?.name || '未选择账号'},
                {label: '自动回复', value: replies.length, hint: `${activeReplies} 条启用规则`}
            ]}/>

            {accounts.length > 0 && (
                <section className="mp-account-context" aria-label="当前公众号账号">
                    <div>
                        <span>当前账号</span>
                        <strong>{selectedAccount?.name || '请选择账号'}</strong>
                        <small>{selectedAccount?.appId || '选择账号后加载关联配置'}</small>
                    </div>
                    <label>
                        <span>切换账号</span>
                        <select value={selectedAccountId}
                                onChange={(event) => setSelectedAccountId(event.target.value)}>
                            {accounts.map((account) => (
                                <option key={account.id}
                                        value={account.id}>{account.name || account.appId || account.id}</option>
                            ))}
                        </select>
                    </label>
                </section>
            )}

            {activeView === 'accounts' && (
                <BackendDataTable
                    ariaLabel="公众号账号列表"
                    columns={ACCOUNT_COLUMNS}
                    rows={accounts}
                    loading={accountsLoading}
                    selectedId={selectedAccountId}
                    onSelect={(row) => setSelectedAccountId(String(row.id))}
                    emptyText="暂无公众号账号"
                    searchPlaceholder="搜索账号名称或 App ID"
                    rowActions={canEdit ? (row) => (
                        <button className="table-link" type="button" onClick={() => setDialog({type: 'account', row})}>
                            编辑
                        </button>
                    ) : undefined}
                />
            )}

            {activeView !== 'accounts' && !selectedAccount && (
                <section className="panel mp-empty-context">
                    <strong>请先创建公众号账号</strong>
                    <p>菜单、素材和自动回复都需要归属到具体公众号账号。</p>
                    {canEdit && (
                        <button className="button primary" type="button"
                                onClick={() => setDialog({type: 'account', row: null})}>
                            新增账号
                        </button>
                    )}
                </section>
            )}

            {activeView === 'menus' && selectedAccount && (
                <BackendDataTable
                    ariaLabel="公众号菜单列表"
                    columns={MENU_COLUMNS}
                    rows={menus}
                    loading={childrenLoading}
                    emptyText="当前账号暂无菜单"
                    searchPlaceholder="搜索菜单名称、类型或地址"
                    rowActions={canEdit ? (row) => (
                        <button className="table-link" type="button"
                                onClick={() => setDialog({type: 'menu', row})}>编辑</button>
                    ) : undefined}
                />
            )}

            {activeView === 'materials' && selectedAccount && (
                <BackendDataTable
                    ariaLabel="公众号素材列表"
                    columns={MATERIAL_COLUMNS}
                    rows={materials}
                    loading={childrenLoading}
                    emptyText="当前账号暂无素材"
                    searchPlaceholder="搜索素材标题、类型或媒体 ID"
                    rowActions={canEdit ? (row) => (
                        <button className="table-link" type="button"
                                onClick={() => setDialog({type: 'material', row})}>编辑</button>
                    ) : undefined}
                />
            )}

            {activeView === 'replies' && selectedAccount && (
                <BackendDataTable
                    ariaLabel="公众号自动回复列表"
                    columns={REPLY_COLUMNS}
                    rows={replies}
                    loading={childrenLoading}
                    emptyText="当前账号暂无自动回复"
                    searchPlaceholder="搜索关键词或回复类型"
                    rowActions={canEdit ? (row) => (
                        <button className="table-link" type="button"
                                onClick={() => setDialog({type: 'reply', row})}>编辑</button>
                    ) : undefined}
                />
            )}

            <RecordDialog
                open={dialog.type === 'account'}
                title={dialog.row ? '编辑公众号账号' : '新增公众号账号'}
                description="敏感凭据不会出现在列表中；编辑时密钥字段留空即可保留现有值。"
                fields={ACCOUNT_FIELDS}
                initialValues={accountInitialValues}
                submitLabel="保存账号"
                busy={actionBusy === `account-${dialog.row?.id || 'create'}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={saveAccount}
            />

            <RecordDialog
                open={dialog.type === 'menu'}
                title={dialog.row ? '编辑公众号菜单' : '新增公众号菜单'}
                fields={menuFields}
                initialValues={dialog.row || {
                    parentId: '0',
                    menuType: 'view',
                    sortNo: 0,
                    status: 'ACTIVE',
                    payload: {}
                }}
                submitLabel="保存菜单"
                busy={actionBusy === `menu-${dialog.row?.id || 'create'}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={saveMenu}
            />

            <RecordDialog
                open={dialog.type === 'material'}
                title={dialog.row ? '编辑公众号素材' : '新增公众号素材'}
                fields={MATERIAL_FIELDS}
                initialValues={dialog.row || {materialType: 'text', status: 'ACTIVE', payload: {}}}
                submitLabel="保存素材"
                busy={actionBusy === `material-${dialog.row?.id || 'create'}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={saveMaterial}
            />

            <RecordDialog
                open={dialog.type === 'reply'}
                title={dialog.row ? '编辑自动回复' : '新增自动回复'}
                fields={REPLY_FIELDS}
                initialValues={dialog.row || {
                    matchType: 'EXACT',
                    replyType: 'TEXT',
                    sortNo: 0,
                    status: 'ACTIVE',
                    replyContent: {text: ''}
                }}
                submitLabel="保存回复"
                busy={actionBusy === `reply-${dialog.row?.id || 'create'}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={saveReply}
            />
        </div>
    )
}
