import {useCallback, useEffect, useMemo, useState} from 'react'
import {useNavigate} from 'react-router-dom'
import {payApi} from '../../api'
import {MetricStrip, StatusBadge, WorkspaceHeader} from '../../components/admin/BackendWorkspace'
import {hasPermission} from '../../domain/admin/permissionPolicy'
import './PayCashierPage.css'

function createOrderNo() {
    return `PAY-${Date.now()}`
}

function formatAmount(value, code = 'CNY') {
    const amount = Number(value)
    if (!Number.isFinite(amount)) return '—'
    try {
        return new Intl.NumberFormat('zh-CN', {style: 'currency', currency: code}).format(amount)
    } catch {
        return `${code} ${amount.toFixed(2)}`
    }
}

function safeChannel(channel) {
    if (!channel || typeof channel !== 'object' || Array.isArray(channel)) return channel
    const safe = {...channel}
    delete safe.appSecret
    return safe
}

export default function PayCashierPage({permissions = [], onError}) {
    const navigate = useNavigate()
    const [channels, setChannels] = useState([])
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [createdOrder, setCreatedOrder] = useState(null)
    const [form, setForm] = useState({
        channelId: '',
        subject: '',
        amount: '0.01',
        currency: 'CNY',
        clientIp: '127.0.0.1'
    })
    const canCreate = hasPermission(permissions, 'biz:pay:order')

    const loadChannels = useCallback(async () => {
        setLoading(true)
        setError('')
        try {
            const rows = await payApi.channels()
            const activeRows = (Array.isArray(rows) ? rows : [])
                .map(safeChannel)
                .filter((item) => String(item.status).toUpperCase() === 'ACTIVE')
            setChannels(activeRows)
            setForm((current) => ({
                ...current,
                channelId: activeRows.some((item) => String(item.id) === String(current.channelId))
                    ? current.channelId
                    : (activeRows[0]?.id || '')
            }))
        } catch (loadError) {
            const message = loadError?.message || '支付渠道加载失败'
            setError(message)
            onError?.(message)
        } finally {
            setLoading(false)
        }
    }, [onError])

    useEffect(() => {
        loadChannels()
    }, [loadChannels])

    const selectedChannel = useMemo(
        () => channels.find((item) => String(item.id) === String(form.channelId)) || null,
        [channels, form.channelId]
    )

    async function submitOrder(event) {
        event.preventDefault()
        if (!canCreate) return
        const amount = Number(form.amount)
        if (!selectedChannel) {
            setError('请选择一个已启用的支付渠道')
            return
        }
        if (!Number.isFinite(amount) || amount <= 0) {
            setError('订单金额必须大于 0')
            return
        }
        setBusy(true)
        setError('')
        try {
            const order = await payApi.createOrder({
                orderNo: createOrderNo(),
                channelId: selectedChannel.id,
                subject: form.subject.trim(),
                amount,
                currency: form.currency,
                status: 'PAYING',
                clientIp: form.clientIp.trim(),
                extra: {source: 'ADMIN_CASHIER'}
            })
            setCreatedOrder(order)
        } catch (createError) {
            const message = createError?.message || '支付订单创建失败'
            setError(message)
            onError?.(message)
        } finally {
            setBusy(false)
        }
    }

  return (
      <section className="pay-cashier-page backend-workspace-page" aria-labelledby="pay-cashier-title">
          <WorkspaceHeader
              breadcrumb="业务中心 / 支付中心"
              title="业务收银台"
              description="选择启用渠道创建真实支付订单，订单创建后可前往支付订单页继续处理回调。"
              status={createdOrder?.status || (loading ? 'RUNNING' : 'ACTIVE')}
              loading={loading}
              onRefresh={loadChannels}
          />

          {error && <div className="error-banner" role="alert">{error}</div>}

          <MetricStrip items={[
              {label: '可用渠道', value: channels.length, hint: selectedChannel?.name || '暂无可用渠道'},
              {label: '订单金额', value: formatAmount(form.amount, form.currency), hint: form.currency},
              {
                  label: '当前渠道',
                  value: selectedChannel?.provider || '未选择',
                  hint: selectedChannel?.name || '请选择渠道'
              },
              {label: '最近订单', value: createdOrder?.orderNo || '尚未创建', hint: createdOrder?.status || '等待提交'}
          ]}/>

          <div className="pay-cashier-grid">
              <form className="panel pay-cashier-form" onSubmit={submitOrder}>
                  <div className="panel-heading">
                      <div>
                          <span className="step">ORDER</span>
                          <h2 id="pay-cashier-title">创建支付订单</h2>
                      </div>
                  </div>

                  <div className="pay-cashier-form-grid">
                      <label>
                          <span>支付渠道</span>
                          <select
                              required
                              value={form.channelId}
                              disabled={loading || busy || channels.length === 0}
                              onChange={(event) => setForm({...form, channelId: event.target.value})}
                          >
                              {channels.length === 0 && <option value="">暂无启用渠道</option>}
                              {channels.map((channel) => (
                                  <option key={channel.id} value={channel.id}>
                                      {channel.name} ({channel.provider})
                                  </option>
                              ))}
                          </select>
                      </label>
                      <label>
                          <span>订单标题</span>
                          <input
                              required
                              value={form.subject}
                              disabled={busy}
                              placeholder="例如：会员续费"
                              onChange={(event) => setForm({...form, subject: event.target.value})}
                          />
                      </label>
                      <label>
                          <span>金额</span>
                          <input
                              required
                              type="number"
                              min="0.01"
                              step="0.01"
                              value={form.amount}
                              disabled={busy}
                              onChange={(event) => setForm({...form, amount: event.target.value})}
                          />
                      </label>
                      <label>
                          <span>币种</span>
                          <select
                              value={form.currency}
                              disabled={busy}
                              onChange={(event) => setForm({...form, currency: event.target.value})}
                          >
                              <option value="CNY">CNY</option>
                              <option value="USD">USD</option>
                              <option value="HKD">HKD</option>
                          </select>
                      </label>
                      <label className="full-field">
                          <span>客户端 IP</span>
                          <input
                              value={form.clientIp}
                              disabled={busy}
                              onChange={(event) => setForm({...form, clientIp: event.target.value})}
                          />
                      </label>
                  </div>

                  <div className="pay-cashier-submit-row">
                      <strong>{formatAmount(form.amount, form.currency)}</strong>
                      <button
                          className="button primary"
                          type="submit"
                          disabled={!canCreate || loading || busy || channels.length === 0}
                      >
                          {busy ? '创建中...' : '创建订单'}
                      </button>
                  </div>
              </form>

              <section className="panel pay-cashier-result" aria-live="polite">
                  <div className="panel-heading">
                      <div>
                          <span className="step">RESULT</span>
                          <h2>订单结果</h2>
                      </div>
                      {createdOrder && <StatusBadge value={createdOrder.status}/>}
                  </div>
                  {createdOrder ? (
                      <dl>
                          <div>
                              <dt>订单号</dt>
                              <dd>{createdOrder.orderNo || '—'}</dd>
                          </div>
                          <div>
                              <dt>订单标题</dt>
                              <dd>{createdOrder.subject || form.subject}</dd>
                          </div>
                          <div>
                              <dt>支付金额</dt>
                              <dd>{formatAmount(createdOrder.amount ?? form.amount, createdOrder.currency || form.currency)}</dd>
                          </div>
                          <div>
                              <dt>支付渠道</dt>
                              <dd>{selectedChannel?.name || createdOrder.channelId || '—'}</dd>
                          </div>
                          <div>
                              <dt>创建时间</dt>
                              <dd>{createdOrder.createTime || '—'}</dd>
                          </div>
                      </dl>
                  ) : (
                      <div className="pay-cashier-empty">
                          <strong>等待创建订单</strong>
                          <p>提交后将在这里显示订单号、状态与金额。</p>
                      </div>
                  )}
                  <button
                      className="button ghost"
                      type="button"
                      disabled={!createdOrder}
                      onClick={() => navigate('/admin/pay/orders')}
                  >
                      查看支付订单
                  </button>
              </section>
          </div>
      </section>
  )
}
