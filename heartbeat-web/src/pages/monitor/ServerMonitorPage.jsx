import {useCallback, useEffect, useState} from 'react'
import {monitorApi} from '../../api'

function formatBytes(bytes) {
  if (bytes === undefined || bytes === null || Number.isNaN(bytes)) return '—'
  const value = Number(bytes)
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  if (value < 1024 * 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`
  return `${(value / 1024 / 1024 / 1024).toFixed(2)} GB`
}

function formatDuration(ms) {
  if (!ms && ms !== 0) return '—'
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  if (days > 0) return `${days} 天 ${hours % 24} 小时`
  if (hours > 0) return `${hours} 小时 ${minutes % 60} 分`
  if (minutes > 0) return `${minutes} 分 ${seconds % 60} 秒`
  return `${seconds} 秒`
}

function MetricCard({ label, value, hint }) {
  return (
      <div className="hb-stat-card">
        <span>{label}</span>
        <strong>{value}</strong>
        {hint && <small>{hint}</small>}
      </div>
  )
}

export default function ServerMonitorPage({ busy, onBusy, onError }) {
  const [metrics, setMetrics] = useState(null)

  const loadMetrics = useCallback(async () => {
    onBusy('monitor-server')
    try {
      const data = await monitorApi.server()
      setMetrics(data)
    } finally {
      onBusy('')
    }
  }, [onBusy])

  useEffect(() => {
    loadMetrics().catch((error) => onError(error.message))
  }, [loadMetrics, onError])

  const cpu = metrics?.cpu || {}
  const memory = metrics?.memory || {}
  const jvm = metrics?.jvm || {}
  const runtime = metrics?.runtime || {}
  const disks = metrics?.disk || []
  const gc = jvm.gc || []

  return (
      <div className="hb-page-card server-monitor-page">
        <div className="server-monitor-header">
          <div>
            <h1>服务监控</h1>
            <p>CPU、内存、JVM 堆栈与磁盘使用情况（数据来自服务端实时采集）。</p>
          </div>
          <button
              type="button"
              className="button ghost"
              disabled={Boolean(busy)}
              onClick={() => loadMetrics().catch((error) => onError(error.message))}
          >
            {busy === 'monitor-server' ? '刷新中…' : '刷新'}
          </button>
        </div>

        {!metrics ? (
            <div className="table-empty">加载监控数据中…</div>
        ) : (
            <>
              <div className="hb-dashboard-grid">
                <MetricCard
                    label="CPU 使用率"
                    value={cpu.systemUsage !== undefined ? `${cpu.systemUsage}%` : '—'}
                    hint={`${cpu.cores || '—'} 核 · 负载 ${cpu.loadAverage ?? '—'}`}
                />
                <MetricCard
                    label="物理内存"
                    value={memory.usage !== undefined ? `${memory.usage}%` : '—'}
                    hint={`已用 ${formatBytes(memory.used)} / ${formatBytes(memory.total)}`}
                />
                <MetricCard
                    label="JVM 堆内存"
                    value={jvm.heapUsage !== undefined ? `${jvm.heapUsage}%` : '—'}
                    hint={`${formatBytes(jvm.heapUsed)} / ${formatBytes(jvm.heapMax)}`}
                />
                <MetricCard
                    label="运行时长"
                    value={formatDuration(runtime.uptimeMs)}
                    hint={runtime.startTime || '—'}
                />
              </div>

              <section className="monitor-section panel">
                <h2>JVM 信息</h2>
                <div className="monitor-kv-grid">
                  <div><span>虚拟机</span><strong>{jvm.name || '—'}</strong></div>
                  <div><span>Java 版本</span><strong>{jvm.version || '—'}</strong></div>
                  <div><span>活动线程</span><strong>{jvm.threads?.live ?? '—'}</strong></div>
                  <div><span>峰值线程</span><strong>{jvm.threads?.peak ?? '—'}</strong></div>
                  <div><span>非堆内存</span><strong>{formatBytes(jvm.nonHeapUsed)}</strong></div>
                  <div><span>运行时总量</span><strong>{formatBytes(jvm.runtimeTotal)}</strong></div>
                </div>
                {gc.length > 0 && (
                    <table className="resource-table monitor-table">
                      <thead>
                        <tr>
                          <th>GC 名称</th>
                          <th>次数</th>
                          <th>耗时 (ms)</th>
                        </tr>
                      </thead>
                      <tbody>
                        {gc.map((item) => (
                            <tr key={item.name}>
                              <td>{item.name}</td>
                              <td>{item.count ?? '—'}</td>
                              <td>{item.timeMs ?? '—'}</td>
                            </tr>
                        ))}
                      </tbody>
                    </table>
                )}
              </section>

              <section className="monitor-section panel">
                <h2>磁盘</h2>
                <table className="resource-table monitor-table">
                  <thead>
                    <tr>
                      <th>盘符</th>
                      <th>总量</th>
                      <th>已用</th>
                      <th>可用</th>
                      <th>使用率</th>
                    </tr>
                  </thead>
                  <tbody>
                    {disks.map((disk) => (
                        <tr key={disk.path}>
                          <td>{disk.path}</td>
                          <td>{formatBytes(disk.total)}</td>
                          <td>{formatBytes(disk.used)}</td>
                          <td>{formatBytes(disk.free)}</td>
                          <td>{disk.usage !== undefined ? `${disk.usage}%` : '—'}</td>
                        </tr>
                    ))}
                  </tbody>
                </table>
              </section>
            </>
        )}
      </div>
  )
}
