import {useEffect, useMemo, useState} from 'react'
import {
    Activity,
    ArrowUpRight,
    CalendarDays,
    CircleAlert,
    CircleCheck,
    Clock3,
    Download,
    Gauge,
    Layers3,
    Play,
    Zap
} from 'lucide-react'
import {useNavigate} from 'react-router-dom'
import {flowApi} from '../api'
import './DashboardPage.css'

const EMPTY_SUMMARY = Object.freeze({
    totalFlows: 0,
    activeFlows: 0,
    publishedFlows: 0,
    draftFlows: 0,
    totalRuns: 0,
    runningRuns: 0,
    waitingRuns: 0,
    successRuns: 0,
    failedRuns: 0,
    canceledRuns: 0,
    averageDurationMs: 0,
    successRate: 0
})

function hasPermission(currentUser, permission) {
    const permissions = new Set((currentUser?.permissions || []).map(String))
    return permissions.has('*') || permissions.has('*:*') || permissions.has(permission)
}

function numberValue(value) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : 0
}

function formatNumber(value) {
    return new Intl.NumberFormat('zh-CN').format(numberValue(value))
}

function formatDuration(value) {
    const milliseconds = numberValue(value)
    if (milliseconds <= 0) return '0 ms'
    if (milliseconds < 1000) return `${Math.round(milliseconds)} ms`
    if (milliseconds < 60000) return `${(milliseconds / 1000).toFixed(1)} s`
    return `${(milliseconds / 60000).toFixed(1)} min`
}

function MetricCard({label, value, hint, Icon, tone}) {
    return (
        <article className={`hb-overview-metric ${tone}`}>
            <div className="hb-overview-metric-topline">
                <span>{label}</span>
                <span className="hb-overview-metric-icon" aria-hidden="true"><Icon size={18}/></span>
            </div>
            <strong>{value}</strong>
            <small>{hint}</small>
        </article>
    )
}

function SuccessGauge({rate}) {
    const safeRate = Math.max(0, Math.min(100, numberValue(rate)))
    return (
        <div className="hb-success-gauge">
            <svg viewBox="0 0 220 128" role="img" aria-label={`执行成功率 ${safeRate.toFixed(1)}%`}>
                <defs>
                    <linearGradient id="dashboard-success-gradient" x1="0" y1="0" x2="1" y2="0">
                        <stop offset="0" stopColor="var(--hb-accent)"/>
                        <stop offset="1" stopColor="#68a8ff"/>
                    </linearGradient>
                </defs>
                <path className="hb-gauge-track" pathLength="100" d="M 22 108 A 88 88 0 0 1 198 108"/>
                <path
                    className="hb-gauge-value"
                    pathLength="100"
                    strokeDasharray={`${safeRate} 100`}
                    d="M 22 108 A 88 88 0 0 1 198 108"
                />
            </svg>
            <div className="hb-success-gauge-value">
                <strong>{safeRate.toFixed(1)}%</strong>
                <span>执行成功率</span>
            </div>
        </div>
    )
}

export default function DashboardPage({currentUser}) {
    const navigate = useNavigate()
    const [rangeDays, setRangeDays] = useState(30)
    const [summary, setSummary] = useState(EMPTY_SUMMARY)
    const [summaryStatus, setSummaryStatus] = useState('idle')
    const displayName = currentUser?.nickname || currentUser?.username || '管理员'
    const canViewFlowSummary = hasPermission(currentUser, 'flow:studio:list')
    const today = new Intl.DateTimeFormat('zh-CN', {
        month: 'long',
        day: 'numeric',
        weekday: 'long'
    }).format(new Date())

    useEffect(() => {
        if (!canViewFlowSummary) {
            setSummary(EMPTY_SUMMARY)
            setSummaryStatus('restricted')
            return undefined
        }

        const controller = new AbortController()
        const startedBefore = new Date()
        const startedAfter = new Date(startedBefore.getTime() - rangeDays * 24 * 60 * 60 * 1000)
        setSummaryStatus('loading')
        flowApi.runSummary({
            startedAfter: startedAfter.toISOString(),
            startedBefore: startedBefore.toISOString()
        }, {signal: controller.signal}).then((result) => {
            setSummary({...EMPTY_SUMMARY, ...(result || {})})
            setSummaryStatus('ready')
        }).catch((error) => {
            if (error?.name === 'AbortError') return
            setSummary(EMPTY_SUMMARY)
            setSummaryStatus('unavailable')
        })
        return () => controller.abort()
    }, [canViewFlowSummary, rangeDays])

    const runBreakdown = useMemo(() => ([
        {label: '成功', value: numberValue(summary.successRuns), tone: 'success'},
        {label: '运行中', value: numberValue(summary.runningRuns), tone: 'running'},
        {label: '等待中', value: numberValue(summary.waitingRuns), tone: 'waiting'},
        {label: '失败', value: numberValue(summary.failedRuns), tone: 'failed'},
        {label: '已取消', value: numberValue(summary.canceledRuns), tone: 'canceled'}
    ]), [summary])
    const maxBreakdown = Math.max(1, ...runBreakdown.map((item) => item.value))
    const pendingRuns = numberValue(summary.runningRuns) + numberValue(summary.waitingRuns)
    const metricPlaceholder = summaryStatus === 'restricted' || summaryStatus === 'unavailable'

    function exportSummary() {
        const blob = new Blob([JSON.stringify({rangeDays, ...summary}, null, 2)], {type: 'application/json'})
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `heartbeat-overview-${rangeDays}d.json`
        link.click()
        URL.revokeObjectURL(url)
    }

    return (
        <div className="hb-page-card hb-overview-page">
            <header className="hb-overview-header">
                <div>
                    <p className="page-breadcrumb">工作台 / 运营总览</p>
                    <h1>欢迎回来，{displayName} <span aria-hidden="true">👋</span></h1>
                    <p>{today}，这里是 HeartBeat 的实时运营与流程概览。</p>
                </div>
                <div className="hb-overview-actions">
                    <label className="hb-range-select">
                        <CalendarDays size={15} aria-hidden="true"/>
                        <span className="sr-only">统计周期</span>
                        <select value={rangeDays} onChange={(event) => setRangeDays(Number(event.target.value))}>
                            <option value={7}>近 7 天</option>
                            <option value={30}>近 30 天</option>
                            <option value={90}>近 90 天</option>
                        </select>
                    </label>
                    <button className="hb-overview-export" type="button" onClick={exportSummary}>
                        <Download size={15} aria-hidden="true"/>
                        导出
                    </button>
                </div>
            </header>

            <section className="hb-overview-metrics" aria-label="关键指标">
                <MetricCard
                    label="活跃流程"
                    value={metricPlaceholder ? '—' : formatNumber(summary.activeFlows)}
                    hint={`${formatNumber(summary.publishedFlows)} 个已发布`}
                    Icon={Layers3}
                    tone="primary"
                />
                <MetricCard
                    label="周期执行"
                    value={metricPlaceholder ? '—' : formatNumber(summary.totalRuns)}
                    hint={`${formatNumber(summary.successRuns)} 次成功完成`}
                    Icon={Zap}
                    tone="dark"
                />
                <MetricCard
                    label="执行成功率"
                    value={metricPlaceholder ? '—' : `${numberValue(summary.successRate).toFixed(1)}%`}
                    hint={`${formatNumber(summary.failedRuns)} 次执行失败`}
                    Icon={Gauge}
                    tone="blue"
                />
                <MetricCard
                    label="待处理任务"
                    value={metricPlaceholder ? '—' : formatNumber(pendingRuns)}
                    hint={`${formatNumber(summary.runningRuns)} 个正在运行`}
                    Icon={Clock3}
                    tone="soft"
                />
            </section>

            <section className="hb-overview-main-grid">
                <article className="hb-overview-panel hb-execution-panel">
                    <header className="hb-overview-panel-heading">
                        <div>
                            <h2>执行分布</h2>
                            <p>不同运行状态的任务数量</p>
                        </div>
                        <span className={`hb-data-state ${summaryStatus}`}>
                            {summaryStatus === 'loading' ? '同步中' : summaryStatus === 'ready' ? '实时数据' : '暂无数据'}
                        </span>
                    </header>
                    <div className="hb-run-chart" role="img" aria-label="流程执行状态柱状图">
                        <div className="hb-chart-grid" aria-hidden="true">
                            <span/><span/><span/><span/>
                        </div>
                        <div className="hb-run-bars">
                            {runBreakdown.map((item) => {
                                const height = item.value === 0 ? 3 : Math.max(12, item.value / maxBreakdown * 100)
                                return (
                                    <div className="hb-run-bar-column" key={item.label}>
                                        <span className="hb-run-bar-value">{formatNumber(item.value)}</span>
                                        <span
                                            className={`hb-run-bar ${item.tone}`}
                                            style={{'--hb-run-bar-height': `${height}%`}}
                                            aria-hidden="true"
                                        />
                                        <span className="hb-run-bar-label">{item.label}</span>
                                    </div>
                                )
                            })}
                        </div>
                    </div>
                    <footer className="hb-chart-footer">
                        <span><Activity size={14}/> 平均耗时 {formatDuration(summary.averageDurationMs)}</span>
                        {canViewFlowSummary && (
                            <button type="button" onClick={() => navigate('/admin/flows/runs')}>
                                查看运行记录 <ArrowUpRight size={14}/>
                            </button>
                        )}
                    </footer>
                </article>

                <article className="hb-overview-panel hb-success-panel">
                    <header className="hb-overview-panel-heading">
                        <div>
                            <h2>流程健康度</h2>
                            <p>已完成执行的成功占比</p>
                        </div>
                        <CircleCheck size={20} aria-hidden="true"/>
                    </header>
                    <SuccessGauge rate={summary.successRate}/>
                    <div className="hb-gauge-summary">
                        <div><span>成功执行</span><strong>{formatNumber(summary.successRuns)}</strong></div>
                        <div><span>失败执行</span><strong>{formatNumber(summary.failedRuns)}</strong></div>
                    </div>
                </article>
            </section>

            <section className="hb-overview-secondary-grid">
                <article className="hb-overview-panel hb-status-panel">
                    <header className="hb-overview-panel-heading">
                        <div>
                            <h2>运行状态</h2>
                            <p>需要关注的流程运行情况</p>
                        </div>
                        <Activity size={20} aria-hidden="true"/>
                    </header>
                    <div className="hb-status-list">
                        <div>
                            <span className="hb-status-icon running"><Play size={16}/></span>
                            <span><strong>正在运行</strong><small>任务仍在处理中</small></span>
                            <b>{formatNumber(summary.runningRuns)}</b>
                        </div>
                        <div>
                            <span className="hb-status-icon waiting"><Clock3 size={16}/></span>
                            <span><strong>等待唤醒</strong><small>正在等待事件或调度</small></span>
                            <b>{formatNumber(summary.waitingRuns)}</b>
                        </div>
                        <div>
                            <span className="hb-status-icon failed"><CircleAlert size={16}/></span>
                            <span><strong>执行失败</strong><small>建议进入运行记录检查</small></span>
                            <b>{formatNumber(summary.failedRuns)}</b>
                        </div>
                    </div>
                </article>

                <article className="hb-overview-panel hb-account-panel">
                    <header className="hb-overview-panel-heading">
                        <div>
                            <h2>当前会话</h2>
                            <p>账号与访问范围</p>
                        </div>
                        <span className="hb-session-live">在线</span>
                    </header>
                    <dl className="hb-account-details">
                        <div>
                            <dt>登录账号</dt>
                            <dd>{currentUser?.username || '已认证'}</dd>
                        </div>
                        <div>
                            <dt>用户 ID</dt>
                            <dd>{currentUser?.id ?? '—'}</dd>
                        </div>
                        <div>
                            <dt>租户 ID</dt>
                            <dd>{currentUser?.tenantId ?? '—'}</dd>
                        </div>
                        <div>
                            <dt>数据权限</dt>
                            <dd>{canViewFlowSummary ? '流程数据可见' : '基础访问'}</dd>
                        </div>
                    </dl>
                </article>
            </section>
        </div>
    )
}
