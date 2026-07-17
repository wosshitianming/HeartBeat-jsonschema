import {useCallback, useEffect, useMemo, useRef, useState} from 'react'
import {monitorApi} from '../../api'
import {hasPermission} from '../../domain/admin/permissionPolicy'
import './SystemMonitorPage.css'

const MONITOR_TABS = [
    {
        key: 'server',
        permission: 'monitor:server:list',
        label: '服务器',
        description: '查看 CPU、内存、JVM、线程和磁盘的实时运行状态。'
    },
    {
        key: 'cache',
        permission: 'monitor:cache:list',
        label: '缓存',
        description: '检查缓存组件启用状态、实现类型和当前缓存空间。'
    },
    {
        key: 'druid',
        permission: 'monitor:druid:list',
        label: '数据源',
        description: '查看连接池容量、活动连接、空闲连接和等待线程。'
    }
]

const MONITOR_LOADERS = {
    server: (options) => monitorApi.server(options),
    cache: (options) => monitorApi.cache(options),
    druid: (options) => monitorApi.druid(options)
}

function normalizeTab(tab, availableTabs = MONITOR_TABS) {
    if (availableTabs.some((item) => item.key === tab)) return tab
    return availableTabs[0]?.key || 'server'
}

function hasValue(value) {
    return value !== undefined && value !== null && value !== ''
}

function toFiniteNumber(value) {
    if (!hasValue(value)) return null
    const number = Number(value)
    return Number.isFinite(number) ? number : null
}

function formatNumber(value) {
    const number = toFiniteNumber(value)
    return number === null ? '—' : new Intl.NumberFormat('zh-CN').format(number)
}

function formatBytes(value) {
    const bytes = toFiniteNumber(value)
    if (bytes === null || bytes < 0) return '—'
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
    if (bytes < 1024 ** 3) return `${(bytes / 1024 ** 2).toFixed(1)} MB`
    return `${(bytes / 1024 ** 3).toFixed(2)} GB`
}

function formatDuration(value) {
    const milliseconds = toFiniteNumber(value)
    if (milliseconds === null || milliseconds < 0) return '—'
    const seconds = Math.floor(milliseconds / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)
    if (days > 0) return `${days} 天 ${hours % 24} 小时`
    if (hours > 0) return `${hours} 小时 ${minutes % 60} 分`
    if (minutes > 0) return `${minutes} 分 ${seconds % 60} 秒`
    return `${seconds} 秒`
}

function formatPercent(value) {
    const number = toFiniteNumber(value)
    return number === null ? '—' : `${number}%`
}

function formatDateTime(value) {
    if (!hasValue(value)) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    return new Intl.DateTimeFormat('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    }).format(date)
}

function classNameTail(value) {
    if (!hasValue(value)) return '—'
    const parts = String(value).split('.')
    return parts[parts.length - 1] || String(value)
}

function clampPercent(value) {
    const number = toFiniteNumber(value)
    if (number === null) return 0
    return Math.min(100, Math.max(0, number))
}

function utilizationPercent(current, maximum) {
    const currentValue = toFiniteNumber(current)
    const maximumValue = toFiniteNumber(maximum)
    if (currentValue === null || maximumValue === null || maximumValue <= 0) return null
    return Math.round((currentValue / maximumValue) * 1000) / 10
}

function MetricCard({label, value, hint, tone = 'neutral', progress}) {
    return (
        <div className="hb-stat-card system-monitor-metric" data-tone={tone}>
            <div className="system-monitor-metric-label">
                <span>{label}</span>
                <i aria-hidden="true"/>
            </div>
            <strong>{value}</strong>
            {hint && <small>{hint}</small>}
            {progress !== undefined && progress !== null && (
                <Meter value={progress} label={`${label} ${formatPercent(progress)}`}/>
            )}
        </div>
    )
}

function Meter({value, label}) {
    const normalized = clampPercent(value)
    const tone = normalized >= 85 ? 'danger' : normalized >= 65 ? 'warning' : 'healthy'
    return (
        <div
            className="system-monitor-meter"
            data-tone={tone}
            role="progressbar"
            aria-label={label}
            aria-valuemin="0"
            aria-valuemax="100"
            aria-valuenow={normalized}
        >
            <span style={{width: `${normalized}%`}}/>
        </div>
    )
}

function DetailGrid({items}) {
    return (
        <dl className="monitor-kv-grid system-monitor-detail-grid">
            {items.map(({label, value, title}) => (
                <div key={label}>
                    <dt>{label}</dt>
                    <dd title={title || (hasValue(value) ? String(value) : undefined)}>
                        {hasValue(value) ? value : '—'}
                    </dd>
                </div>
            ))}
        </dl>
    )
}

function EmptyState({title, description}) {
    return (
        <div className="system-monitor-empty" role="status">
            <strong>{title}</strong>
            <p>{description}</p>
        </div>
    )
}

function ServerMonitorView({data}) {
    const cpu = data?.cpu || {}
    const memory = data?.memory || {}
    const jvm = data?.jvm || {}
    const runtime = data?.runtime || {}
    const gc = Array.isArray(jvm.gc) ? jvm.gc : []
    const disks = Array.isArray(data?.disk) ? data.disk : []
    const hasServerData = [cpu, memory, jvm, runtime].some((item) => Object.keys(item).length > 0)
        || disks.length > 0

    if (!hasServerData) {
        return (
            <EmptyState
                title="暂无服务器指标"
                description="服务端已响应，但没有返回可展示的 CPU、内存、JVM 或磁盘数据。"
            />
        )
    }

    return (
        <div className="system-monitor-content-stack">
            <div className="hb-dashboard-grid system-monitor-summary-grid">
                <MetricCard
                    label="CPU 使用率"
                    value={formatPercent(cpu.systemUsage)}
                    hint={`${formatNumber(cpu.cores)} 核 · 系统负载 ${hasValue(cpu.loadAverage) ? cpu.loadAverage : '—'}`}
                    progress={toFiniteNumber(cpu.systemUsage)}
                    tone="blue"
                />
                <MetricCard
                    label="物理内存"
                    value={formatPercent(memory.usage)}
                    hint={`${formatBytes(memory.used)} / ${formatBytes(memory.total)}`}
                    progress={toFiniteNumber(memory.usage)}
                    tone="green"
                />
                <MetricCard
                    label="JVM 堆内存"
                    value={formatPercent(jvm.heapUsage)}
                    hint={`${formatBytes(jvm.heapUsed)} / ${formatBytes(jvm.heapMax)}`}
                    progress={toFiniteNumber(jvm.heapUsage)}
                    tone="amber"
                />
                <MetricCard
                    label="运行时长"
                    value={formatDuration(runtime.uptimeMs)}
                    hint={`启动于 ${formatDateTime(runtime.startTime)}`}
                    tone="violet"
                />
            </div>

            <div className="system-monitor-section-grid">
                <section className="panel system-monitor-section" aria-labelledby="monitor-jvm-heading">
                    <div className="panel-heading">
                        <div>
                            <span className="step">RUNTIME</span>
                            <h2 id="monitor-jvm-heading">JVM 与线程</h2>
                        </div>
                    </div>
                    <DetailGrid items={[
                        {label: '虚拟机', value: jvm.name},
                        {label: 'Java 版本', value: jvm.version},
                        {label: '活动线程', value: formatNumber(jvm.threads?.live)},
                        {label: '守护线程', value: formatNumber(jvm.threads?.daemon)},
                        {label: '峰值线程', value: formatNumber(jvm.threads?.peak)},
                        {label: '非堆内存', value: formatBytes(jvm.nonHeapUsed)},
                        {label: '运行时内存', value: formatBytes(jvm.runtimeTotal)},
                        {label: '采集时间', value: formatDateTime(data?.timestamp)}
                    ]}/>
                </section>

                <section className="panel system-monitor-section" aria-labelledby="monitor-gc-heading">
                    <div className="panel-heading">
                        <div>
                            <span className="step">GC</span>
                            <h2 id="monitor-gc-heading">垃圾回收</h2>
                        </div>
                        <span className="status-pill">{gc.length} 个收集器</span>
                    </div>
                    {gc.length === 0 ? (
                        <EmptyState title="暂无 GC 统计" description="当前 JVM 没有返回垃圾回收器指标。"/>
                    ) : (
                        <div className="system-monitor-table-wrap">
                            <table className="resource-table monitor-table">
                                <thead>
                                <tr>
                                    <th scope="col">收集器</th>
                                    <th scope="col">执行次数</th>
                                    <th scope="col">累计耗时</th>
                                </tr>
                                </thead>
                                <tbody>
                                {gc.map((item, index) => (
                                    <tr key={`${item.name || 'gc'}-${index}`}>
                                        <td>{item.name || '未命名收集器'}</td>
                                        <td>{formatNumber(item.count)}</td>
                                        <td>{hasValue(item.timeMs) ? `${formatNumber(item.timeMs)} ms` : '—'}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </section>
            </div>

            <section className="panel system-monitor-section" aria-labelledby="monitor-disk-heading">
                <div className="panel-heading">
                    <div>
                        <span className="step">STORAGE</span>
                        <h2 id="monitor-disk-heading">磁盘使用</h2>
                    </div>
                    <span className="status-pill">{disks.length} 个挂载点</span>
                </div>
                {disks.length === 0 ? (
                    <EmptyState title="暂无磁盘信息" description="当前运行环境没有返回可读取的文件系统指标。"/>
                ) : (
                    <div className="system-monitor-table-wrap">
                        <table className="resource-table monitor-table">
                            <thead>
                            <tr>
                                <th scope="col">挂载点</th>
                                <th scope="col">容量</th>
                                <th scope="col">已使用</th>
                                <th scope="col">可用</th>
                                <th scope="col">使用率</th>
                            </tr>
                            </thead>
                            <tbody>
                            {disks.map((disk, index) => (
                                <tr key={`${disk.path || 'disk'}-${index}`}>
                                    <td><code>{disk.path || '—'}</code></td>
                                    <td>{formatBytes(disk.total)}</td>
                                    <td>{formatBytes(disk.used)}</td>
                                    <td>{formatBytes(disk.free)}</td>
                                    <td className="system-monitor-usage-cell">
                                        <span>{formatPercent(disk.usage)}</span>
                                        {toFiniteNumber(disk.usage) !== null && (
                                            <Meter value={disk.usage} label={`${disk.path || '磁盘'}使用率`}/>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>
        </div>
    )
}

function CacheMonitorView({data}) {
    const enabled = data?.enabled === true
    const caches = Array.isArray(data?.caches) ? data.caches : []
    const provider = data?.provider

    return (
        <div className="system-monitor-content-stack">
            <div className="hb-dashboard-grid system-monitor-summary-grid">
                <MetricCard
                    label="缓存状态"
                    value={enabled ? '已启用' : '未启用'}
                    hint={enabled ? '缓存管理器运行正常' : '当前没有可用缓存管理器'}
                    tone={enabled ? 'green' : 'neutral'}
                />
                <MetricCard
                    label="缓存空间"
                    value={formatNumber(caches.length)}
                    hint="服务端已注册的缓存名称"
                    tone="blue"
                />
                <MetricCard
                    label="缓存提供方"
                    value={classNameTail(provider)}
                    hint={hasValue(provider) ? String(provider) : '未返回实现信息'}
                    tone="violet"
                />
                <MetricCard
                    label="可检查空间"
                    value={formatNumber(caches.filter((item) => hasValue(item?.implementation)).length)}
                    hint="已返回底层实现的缓存空间"
                    tone="amber"
                />
            </div>

            <section className="panel system-monitor-section" aria-labelledby="monitor-cache-heading">
                <div className="panel-heading">
                    <div>
                        <span className="step">CACHE</span>
                        <h2 id="monitor-cache-heading">缓存空间</h2>
                    </div>
                    <span className={`system-monitor-health ${enabled ? 'healthy' : 'muted'}`}>
              <i aria-hidden="true"/>{enabled ? '运行中' : '未启用'}
            </span>
                </div>

                {!enabled ? (
                    <EmptyState
                        title="缓存组件未启用"
                        description="应用当前未注册 CacheManager；启用缓存组件后，这里会列出缓存名称和底层实现。"
                    />
                ) : caches.length === 0 ? (
                    <EmptyState
                        title="暂无缓存空间"
                        description="缓存管理器已经启用，但当前尚未注册任何缓存名称。"
                    />
                ) : (
                    <div className="system-monitor-table-wrap">
                        <table className="resource-table monitor-table">
                            <thead>
                            <tr>
                                <th scope="col">缓存名称</th>
                                <th scope="col">底层实现</th>
                                <th scope="col">状态</th>
                            </tr>
                            </thead>
                            <tbody>
                            {caches.map((cache, index) => (
                                <tr key={`${cache?.name || 'cache'}-${index}`}>
                                    <td><strong>{cache?.name || '未命名缓存'}</strong></td>
                                    <td title={cache?.implementation || undefined}>
                                        {classNameTail(cache?.implementation)}
                                    </td>
                                    <td><span className="system-monitor-health healthy"><i
                                        aria-hidden="true"/>可用</span></td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            <section className="panel system-monitor-section" aria-labelledby="monitor-cache-runtime-heading">
                <div className="panel-heading">
                    <div>
                        <span className="step">PROVIDER</span>
                        <h2 id="monitor-cache-runtime-heading">运行实现</h2>
                    </div>
                </div>
                <DetailGrid items={[
                    {label: '是否启用', value: enabled ? '是' : '否'},
                    {label: '管理器类型', value: classNameTail(provider), title: provider},
                    {label: '完整类名', value: provider || 'NONE', title: provider},
                    {label: '缓存空间数', value: formatNumber(caches.length)}
                ]}/>
            </section>
        </div>
    )
}

function DataSourceMonitorView({data}) {
    const enabled = data?.enabled === true
    const maximum = toFiniteNumber(data?.maximumPoolSize)
    const minimum = toFiniteNumber(data?.minimumIdle)
    const active = toFiniteNumber(data?.activeConnections)
    const idle = toFiniteNumber(data?.idleConnections)
    const total = toFiniteNumber(data?.totalConnections)
    const waiting = toFiniteNumber(data?.threadsAwaitingConnection)
    const utilization = utilizationPercent(active, maximum)

    return (
        <div className="system-monitor-content-stack">
            <div className="hb-dashboard-grid system-monitor-summary-grid">
                <MetricCard
                    label="活动连接"
                    value={formatNumber(active)}
                    hint={maximum === null ? '未返回最大连接数' : `最大连接 ${formatNumber(maximum)}`}
                    progress={utilization}
                    tone="blue"
                />
                <MetricCard
                    label="空闲连接"
                    value={formatNumber(idle)}
                    hint={minimum === null ? '未返回最小空闲数' : `最小空闲 ${formatNumber(minimum)}`}
                    tone="green"
                />
                <MetricCard
                    label="连接总数"
                    value={formatNumber(total)}
                    hint={enabled ? '当前连接池实例' : '数据源未启用'}
                    tone="violet"
                />
                <MetricCard
                    label="等待线程"
                    value={formatNumber(waiting)}
                    hint={waiting !== null && waiting > 0 ? '存在等待获取连接的线程' : '当前无连接等待'}
                    tone={waiting !== null && waiting > 0 ? 'amber' : 'neutral'}
                />
            </div>

            {!enabled ? (
                <section className="panel system-monitor-section">
                    <EmptyState
                        title="数据源未启用"
                        description="应用当前未注册可监控的数据源；启用连接池后，这里会显示容量和实时连接指标。"
                    />
                </section>
            ) : (
                <div className="system-monitor-section-grid">
                    <section className="panel system-monitor-section" aria-labelledby="monitor-pool-heading">
                        <div className="panel-heading">
                            <div>
                                <span className="step">POOL</span>
                                <h2 id="monitor-pool-heading">连接池容量</h2>
                            </div>
                            <span className="system-monitor-health healthy"><i aria-hidden="true"/>运行中</span>
                        </div>
                        <div className="system-monitor-capacity">
                            <div>
                                <span>活动连接占最大容量</span>
                                <strong>{utilization === null ? '—' : formatPercent(utilization)}</strong>
                            </div>
                            {utilization === null ? (
                                <p>当前数据源实现没有提供容量占用指标。</p>
                            ) : (
                                <Meter value={utilization} label="活动连接占最大容量"/>
                            )}
                        </div>
                        <DetailGrid items={[
                            {label: '最大连接数', value: formatNumber(maximum)},
                            {label: '最小空闲数', value: formatNumber(minimum)},
                            {label: '当前连接总数', value: formatNumber(total)},
                            {label: '等待线程数', value: formatNumber(waiting)}
                        ]}/>
                    </section>

                    <section className="panel system-monitor-section" aria-labelledby="monitor-datasource-heading">
                        <div className="panel-heading">
                            <div>
                                <span className="step">DATASOURCE</span>
                                <h2 id="monitor-datasource-heading">数据源实现</h2>
                            </div>
                        </div>
                        <DetailGrid items={[
                            {label: '启用状态', value: enabled ? '已启用' : '未启用'},
                            {
                                label: '实现类型',
                                value: classNameTail(data?.implementation),
                                title: data?.implementation
                            },
                            {label: '活动连接', value: formatNumber(active)},
                            {label: '空闲连接', value: formatNumber(idle)},
                            {label: '连接总数', value: formatNumber(total)},
                            {label: '等待连接', value: formatNumber(waiting)}
                        ]}/>
                    </section>
                </div>
            )}
        </div>
    )
}

function MonitorView({tab, data}) {
    if (tab === 'cache') return <CacheMonitorView data={data}/>
    if (tab === 'druid') return <DataSourceMonitorView data={data}/>
    return <ServerMonitorView data={data}/>
}

export default function SystemMonitorPage({initialTab = 'server', permissions = [], busy = '', onBusy, onError}) {
    const availableTabs = useMemo(() => (
        MONITOR_TABS.filter((tab) => hasPermission(permissions, tab.permission))
    ), [permissions])
    const [activeTab, setActiveTab] = useState(() => normalizeTab(initialTab, availableTabs))
    const [dataByTab, setDataByTab] = useState({})
    const [stateByTab, setStateByTab] = useState({})
    const [errorByTab, setErrorByTab] = useState({})
    const [updatedAtByTab, setUpdatedAtByTab] = useState({})
    const controllersRef = useRef({})
    const pendingCountRef = useRef(0)

    useEffect(() => {
        setActiveTab(normalizeTab(initialTab, availableTabs))
    }, [availableTabs, initialTab])

    useEffect(() => () => {
        Object.values(controllersRef.current).forEach((controller) => controller.abort())
    }, [])

    const loadTab = useCallback(async (tab) => {
        if (availableTabs.length === 0) return
        const normalizedTab = normalizeTab(tab, availableTabs)
        controllersRef.current[normalizedTab]?.abort()
        const controller = new AbortController()
        controllersRef.current[normalizedTab] = controller
        pendingCountRef.current += 1
        setStateByTab((previous) => ({...previous, [normalizedTab]: 'loading'}))
        setErrorByTab((previous) => ({...previous, [normalizedTab]: ''}))
        onBusy?.(`monitor-${normalizedTab}`)

        try {
            const data = await MONITOR_LOADERS[normalizedTab]({signal: controller.signal})
            if (controller.signal.aborted) return
            setDataByTab((previous) => ({...previous, [normalizedTab]: data}))
            setUpdatedAtByTab((previous) => ({...previous, [normalizedTab]: new Date()}))
            setStateByTab((previous) => ({...previous, [normalizedTab]: 'success'}))
        } catch (error) {
            if (controller.signal.aborted || error?.name === 'AbortError') return
            const message = error?.message || '监控数据加载失败'
            setErrorByTab((previous) => ({...previous, [normalizedTab]: message}))
            setStateByTab((previous) => ({...previous, [normalizedTab]: 'error'}))
            onError?.(message)
        } finally {
            if (controllersRef.current[normalizedTab] === controller) {
                delete controllersRef.current[normalizedTab]
            }
            pendingCountRef.current = Math.max(0, pendingCountRef.current - 1)
            if (pendingCountRef.current === 0) onBusy?.('')
        }
    }, [availableTabs, onBusy, onError])

    useEffect(() => {
        if (!stateByTab[activeTab]) loadTab(activeTab)
    }, [activeTab, loadTab, stateByTab])

    const activeDefinition = useMemo(
        () => availableTabs.find((tab) => tab.key === activeTab) || availableTabs[0] || MONITOR_TABS[0],
        [activeTab, availableTabs]
    )
    const activeState = stateByTab[activeTab] || 'idle'
    const activeError = errorByTab[activeTab]
    const activeData = dataByTab[activeTab]
    const updatedAt = updatedAtByTab[activeTab]
    const isLoading = activeState === 'loading'
    const externalBusy = Boolean(busy) && busy !== `monitor-${activeTab}`

    if (availableTabs.length === 0) {
        return (
            <section className="system-monitor-page">
                <div className="panel system-monitor-error" role="alert">
                    <div>
                        <strong>暂无系统监控权限</strong>
                        <p>当前账号未分配服务器、缓存或数据源监控权限。</p>
                    </div>
                </div>
            </section>
        )
    }

    return (
        <section className="system-monitor-page" aria-labelledby="system-monitor-title">
            <header className="module-page-header system-monitor-header">
                <div>
                    <p className="page-breadcrumb">运维监控 / 系统监控</p>
                    <h1 id="system-monitor-title">系统监控</h1>
                    <p>{activeDefinition.description}</p>
                </div>
                <div className="system-monitor-header-actions">
            <span className="system-monitor-updated" aria-live="polite">
              {updatedAt ? `更新于 ${formatDateTime(updatedAt)}` : '等待首次采集'}
            </span>
                    <button
                        type="button"
                        className="button ghost system-monitor-refresh"
                        disabled={isLoading || externalBusy}
                        onClick={() => loadTab(activeTab)}
                    >
                        <span aria-hidden="true">↻</span>
                        {isLoading ? '刷新中' : '刷新'}
                    </button>
                </div>
            </header>

            <div className="system-monitor-command-bar">
                <div className="system-monitor-tabs" role="tablist" aria-label="监控类型">
                    {availableTabs.map((tab) => {
                        const selected = tab.key === activeTab
                        const tabState = stateByTab[tab.key]
                        return (
                            <button
                                key={tab.key}
                                id={`system-monitor-tab-${tab.key}`}
                                type="button"
                                role="tab"
                                aria-selected={selected}
                                aria-controls="system-monitor-panel"
                                className={selected ? 'active' : ''}
                                onClick={() => setActiveTab(tab.key)}
                            >
                                <span>{tab.label}</span>
                                <i data-state={tabState || 'idle'} aria-hidden="true"/>
                            </button>
                        )
                    })}
                </div>
                <span className="system-monitor-tab-caption">实时采集，不自动轮询</span>
            </div>

            <div
                id="system-monitor-panel"
                className="system-monitor-panel"
                role="tabpanel"
                aria-labelledby={`system-monitor-tab-${activeTab}`}
                aria-busy={isLoading}
            >
                {(activeState === 'idle' || isLoading) && activeData === undefined ? (
                    <div className="system-monitor-loading" role="status" aria-live="polite">
                        <span aria-hidden="true"/>
                        <strong>正在采集{activeDefinition.label}监控数据</strong>
                        <p>数据返回后会自动整理为摘要指标和结构化明细。</p>
                    </div>
                ) : activeState === 'error' && activeData === undefined ? (
                    <div className="panel system-monitor-error" role="alert">
                        <div>
                            <strong>{activeDefinition.label}监控加载失败</strong>
                            <p>{activeError}</p>
                        </div>
                        <button type="button" className="button primary" onClick={() => loadTab(activeTab)}>
                            重新加载
                        </button>
                    </div>
                ) : (
                    <>
                        {activeError && (
                            <div className="system-monitor-inline-error" role="alert">
                                <span>{activeError}</span>
                                <button type="button" className="text-button" onClick={() => loadTab(activeTab)}>重试
                                </button>
                            </div>
                        )}
                        <MonitorView tab={activeTab} data={activeData}/>
                    </>
                )}
            </div>
        </section>
    )
}
