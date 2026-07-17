import {useCallback, useEffect, useMemo, useState} from 'react'
import {payApi} from '../../api'
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
    {key: 'channels', label: '支付渠道'},
    {key: 'orders', label: '支付订单'},
    {key: 'notifications', label: '通知日志'}
]

const CHANNEL_FIELDS = [
    {name: 'name', label: '渠道名称', required: true},
    {name: 'provider', label: '服务商', required: true, placeholder: 'WECHAT / ALIPAY / MOCK'},
    {name: 'appId', label: '应用 ID'},
    {name: 'appSecret', label: '应用密钥', type: 'password'},
    {name: 'status', label: '状态', type: 'select', options: ['ACTIVE', 'DISABLED'], defaultValue: 'ACTIVE'},
    {name: 'sortNo', label: '排序', type: 'number', defaultValue: 0},
    {name: 'config', label: '扩展配置', type: 'json', fullWidth: true, defaultValue: {}}
]

const NOTIFY_FIELDS = [
    {name: 'status', label: '支付状态', type: 'select', options: ['PAID', 'CLOSED'], defaultValue: 'PAID'},
    {name: 'payload', label: '通知报文', type: 'json', fullWidth: true, defaultValue: {}},
    {
        name: 'appSecret',
        label: '渠道签名密钥',
        type: 'password',
        required: true,
        fullWidth: true,
        hint: '仅用于本次浏览器端 HMAC 签名，不会保存到页面列表或本地存储。'
    }
]

const NOTIFIABLE_ORDER_STATUSES = new Set(['CREATED', 'PAYING'])

function sanitizeChannel(channel) {
    if (!channel || typeof channel !== 'object' || Array.isArray(channel)) return channel
    const safeChannel = {...channel}
    delete safeChannel.appSecret
    return safeChannel
}

function currency(value, code = 'CNY') {
    const amount = Number(value)
    if (!Number.isFinite(amount)) return '—'
    try {
        return new Intl.NumberFormat('zh-CN', {style: 'currency', currency: code || 'CNY'}).format(amount)
    } catch {
        return `${code || ''} ${amount.toFixed(2)}`.trim()
    }
}

async function signPayload(payload, secret) {
    if (!globalThis.crypto?.subtle || typeof TextEncoder === 'undefined') {
        throw new Error('当前浏览器无法使用 Web Crypto，请通过 HTTPS 或 localhost 打开管理台')
    }
    const encoder = new TextEncoder()
    const key = await globalThis.crypto.subtle.importKey(
        'raw',
        encoder.encode(secret),
        {name: 'HMAC', hash: 'SHA-256'},
        false,
        ['sign']
    )
    const signature = await globalThis.crypto.subtle.sign('HMAC', key, encoder.encode(payload))
    return Array.from(new Uint8Array(signature), (value) => value.toString(16).padStart(2, '0')).join('')
}

export default function PayWorkspacePage({initialView = 'orders', permissions = [], onError}) {
    const [activeView, setActiveView] = useState(initialView)
    const [channels, setChannels] = useState([])
    const [orders, setOrders] = useState([])
    const [logs, setLogs] = useState([])
    const [loading, setLoading] = useState(true)
    const [actionBusy, setActionBusy] = useState('')
    const [localError, setLocalError] = useState('')
    const [dialog, setDialog] = useState({type: '', row: null})

    useEffect(() => setActiveView(initialView), [initialView])

    const reportError = useCallback((error) => {
        const message = error?.message || '支付操作失败'
        setLocalError(message)
        onError?.(message)
    }, [onError])

    const loadAll = useCallback(async () => {
        setLoading(true)
        setLocalError('')
        const results = await Promise.allSettled([payApi.channels(), payApi.orders(), payApi.notifyLogs()])
        if (results[0].status === 'fulfilled') {
            const channelRows = Array.isArray(results[0].value) ? results[0].value : []
            setChannels(channelRows.map(sanitizeChannel))
        }
        if (results[1].status === 'fulfilled') setOrders(results[1].value || [])
        if (results[2].status === 'fulfilled') setLogs(results[2].value || [])
        const rejected = results.find((item) => item.status === 'rejected')
        if (rejected) reportError(rejected.reason)
        setLoading(false)
    }, [reportError])

    useEffect(() => {
        loadAll()
    }, [loadAll])

    const tabs = useMemo(() => {
        const counts = {channels: channels.length, orders: orders.length, notifications: logs.length}
        return TAB_ITEMS.map((item) => ({...item, count: counts[item.key]}))
    }, [channels.length, logs.length, orders.length])

    const activeChannels = useMemo(
        () => channels.filter((item) => String(item.status).toUpperCase() === 'ACTIVE'),
        [channels]
    )
    const orderFields = useMemo(() => [
        {name: 'orderNo', label: '订单号', required: true},
        {
            name: 'channelId',
            label: '支付渠道',
            type: 'select',
            required: true,
            options: activeChannels.length > 0
                ? activeChannels.map((item) => ({value: item.id, label: `${item.name} (${item.provider})`}))
                : [{value: '', label: '暂无启用的支付渠道'}]
        },
        {name: 'subject', label: '订单标题', required: true},
        {name: 'amount', label: '金额', type: 'number', required: true},
        {name: 'currency', label: '币种', type: 'select', options: ['CNY', 'USD', 'HKD'], defaultValue: 'CNY'},
        {name: 'status', label: '初始状态', type: 'select', options: ['PAYING', 'CREATED'], defaultValue: 'PAYING'},
        {name: 'clientIp', label: '客户端 IP', placeholder: '127.0.0.1'},
        {name: 'extra', label: '订单扩展数据', type: 'json', fullWidth: true, defaultValue: {}}
    ], [activeChannels])

    const paidOrders = orders.filter((item) => String(item.status).toUpperCase() === 'PAID')
    const paidAmounts = paidOrders.reduce((totals, item) => {
        const amount = Number(item.amount)
        if (!Number.isFinite(amount)) return totals
        const code = String(item.currency || 'CNY').toUpperCase()
        totals[code] = (totals[code] || 0) + amount
        return totals
    }, {})
    const paidAmountEntries = Object.entries(paidAmounts)
    const paidAmountValue = paidAmountEntries.length === 0
        ? '—'
        : paidAmountEntries.length === 1
            ? currency(paidAmountEntries[0][1], paidAmountEntries[0][0])
            : `${paidAmountEntries.length} 个币种`
    const paidAmountHint = paidAmountEntries.length > 0
        ? paidAmountEntries.map(([code, amount]) => currency(amount, code)).join(' · ')
        : '暂无已支付金额'
    const enabledChannels = activeChannels.length
    const canEditChannels = hasPermission(permissions, 'biz:pay:edit')
    const canCreateOrders = hasPermission(permissions, 'biz:pay:order')
    const canNotify = hasPermission(permissions, 'biz:pay:notify')

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

    async function saveChannel(values) {
        const rowId = dialog.row?.id
        if (!rowId && !String(values.appSecret || '').trim()) {
            throw new Error('新增支付渠道时必须填写应用密钥')
        }
        const payload = {
            ...values,
            sortNo: Number(values.sortNo || 0),
            config: parseJsonField(values.config, {})
        }
        if (rowId && !payload.appSecret) delete payload.appSecret
        await execute(`channel-${rowId || 'create'}`, () => (
            rowId ? payApi.updateChannel(rowId, payload) : payApi.createChannel(payload)
        ), true)
    }

    async function createOrder(values) {
        const channelAvailable = activeChannels.some((item) => String(item.id) === String(values.channelId))
        if (!channelAvailable) throw new Error('请选择一个已启用的支付渠道')
        const amount = Number(values.amount)
        if (!Number.isFinite(amount) || amount <= 0) throw new Error('订单金额必须大于 0')
        await execute('create-order', () => payApi.createOrder({
            ...values,
            amount,
            extra: parseJsonField(values.extra, {})
        }), true)
    }

    async function mockNotify(values) {
        const payload = JSON.stringify(parseJsonField(values.payload, {}))
        const signature = await signPayload(payload, String(values.appSecret || ''))
        await execute(`notify-${dialog.row?.orderNo}`, async () => {
            const result = await payApi.mockNotify(dialog.row.orderNo, {
                status: values.status,
                payload,
                signature
            })
            if (String(result?.signatureValid || '').toUpperCase() !== 'SUCCESS') {
                throw new Error('模拟回调验签失败，请检查支付渠道密钥配置')
            }
            return result
        }, true)
    }

    const notifyFields = useMemo(() => {
        const statusOptions = dialog.row?.status === 'CREATED' ? ['PAYING', 'CLOSED'] : ['PAID', 'CLOSED']
        return NOTIFY_FIELDS.map((field) => field.name === 'status' ? {...field, options: statusOptions} : field)
    }, [dialog.row?.status])

    const channelColumns = [
        {key: 'name', label: '渠道名称'},
        {key: 'provider', label: '服务商'},
        {key: 'appId', label: '应用 ID'},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'sortNo', label: '排序'},
        {key: 'updateTime', label: '更新时间'}
    ]
    const orderColumns = [
        {key: 'orderNo', label: '订单号'},
        {key: 'subject', label: '订单标题'},
        {key: 'channelId', label: '渠道 ID'},
        {key: 'amount', label: '金额', render: (value, row) => currency(value, row.currency)},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'createTime', label: '创建时间'}
    ]
    const logColumns = [
        {key: 'orderNo', label: '订单号'},
        {key: 'provider', label: '服务商'},
        {key: 'notifyId', label: '通知 ID'},
        {key: 'signatureValid', label: '签名有效'},
        {key: 'status', label: '状态', render: (value) => <StatusBadge value={value}/>},
        {key: 'createTime', label: '通知时间'}
    ]

    const createAction = activeView === 'channels' && canEditChannels
        ? <button className="button primary" type="button"
                  onClick={() => setDialog({type: 'channel', row: null})}>新增渠道</button>
        : activeView === 'orders' && canCreateOrders
            ? <button className="button primary" type="button" disabled={activeChannels.length === 0}
                      onClick={() => setDialog({type: 'order', row: null})}>创建订单</button>
            : null

    return (
        <div className="hb-page-card backend-workspace-page">
            <WorkspaceHeader
                breadcrumb="业务中心 / 支付中心"
                title="支付运营"
                description="将支付渠道、业务订单和异步通知集中在同一工作区，支持渠道维护、订单创建与回调联调。"
                status="真实数据"
                loading={loading}
                onRefresh={loadAll}
                actions={createAction}
            />
            {localError && <div className="error-banner" role="alert">{localError}</div>}
            <WorkspaceTabs items={tabs} activeKey={activeView} onChange={setActiveView}/>
            <MetricStrip items={[
                {label: '启用渠道', value: enabledChannels, hint: `共 ${channels.length} 个渠道`},
                {label: '支付订单', value: orders.length, hint: `${paidOrders.length} 笔已支付`},
                {label: '已支付金额', value: paidAmountValue, hint: paidAmountHint},
                {label: '通知记录', value: logs.length, hint: '支付回调审计'}
            ]}/>

            {activeView === 'channels' && (
                <BackendDataTable
                    ariaLabel="支付渠道列表"
                    columns={channelColumns}
                    rows={channels}
                    loading={loading}
                    emptyText="暂无支付渠道，请先新增渠道"
                    searchPlaceholder="搜索渠道名称或服务商"
                    rowActions={canEditChannels ? (row) => (
                        <button className="table-link" type="button" onClick={() => setDialog({type: 'channel', row})}>
                            编辑
                        </button>
                    ) : undefined}
                />
            )}

            {activeView === 'orders' && (
                <BackendDataTable
                    ariaLabel="支付订单列表"
                    columns={orderColumns}
                    rows={orders}
                    loading={loading}
                    emptyText="暂无支付订单"
                    searchPlaceholder="搜索订单号或标题"
                    rowActions={canNotify ? (row) => (
                        NOTIFIABLE_ORDER_STATUSES.has(String(row.status).toUpperCase()) ? (
                            <button className="table-link" type="button"
                                    onClick={() => setDialog({type: 'notify', row})}>
                                模拟回调
                            </button>
                        ) : null
                    ) : undefined}
                />
            )}

            {activeView === 'notifications' && (
                <BackendDataTable
                    ariaLabel="支付通知日志"
                    columns={logColumns}
                    rows={logs}
                    loading={loading}
                    emptyText="暂无支付通知记录"
                    searchPlaceholder="搜索订单号或通知 ID"
                />
            )}

            <RecordDialog
                open={dialog.type === 'channel'}
                title={dialog.row ? '编辑支付渠道' : '新增支付渠道'}
                description="应用密钥会提交到后端保存，列表中仅展示非敏感信息。"
                fields={CHANNEL_FIELDS}
                initialValues={dialog.row || {status: 'ACTIVE', sortNo: 0, config: {}}}
                submitLabel="保存渠道"
                busy={actionBusy === `channel-${dialog.row?.id || 'create'}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={saveChannel}
            />

            <RecordDialog
                open={dialog.type === 'order'}
                title="创建支付订单"
                description="订单保存后可从列表触发模拟通知，便于联调支付结果处理。"
                fields={orderFields}
                initialValues={{
                    orderNo: `PAY-${Date.now()}`,
                    channelId: activeChannels[0]?.id || '',
                    amount: 0.01,
                    currency: 'CNY',
                    status: 'PAYING',
                    clientIp: '127.0.0.1',
                    extra: {}
                }}
                submitLabel="创建订单"
                busy={actionBusy === 'create-order'}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={createOrder}
            />

            <RecordDialog
                open={dialog.type === 'notify'}
                title={`模拟支付回调：${dialog.row?.orderNo || ''}`}
                description="浏览器会使用本次输入的渠道密钥生成 HMAC-SHA256 签名，再调用现有模拟通知接口写入通知日志。"
                fields={notifyFields}
                initialValues={{
                    status: dialog.row?.status === 'CREATED' ? 'PAYING' : 'PAID',
                    payload: {orderNo: dialog.row?.orderNo},
                    appSecret: ''
                }}
                submitLabel="发送模拟通知"
                busy={actionBusy === `notify-${dialog.row?.orderNo}`}
                onClose={() => setDialog({type: '', row: null})}
                onSubmit={mockNotify}
            />
        </div>
    )
}
