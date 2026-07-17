import {memo, useEffect, useMemo, useState} from 'react'
import {
    AlertCircle,
    Braces,
    CheckCircle2,
    ChevronDown,
    ChevronUp,
    CircleDot,
    Clipboard,
    Code2,
    FileCheck2,
    LoaderCircle,
    Play,
    TerminalSquare
} from 'lucide-react'

function pretty(value) {
    if (value == null) return ''
    if (typeof value === 'string') return value
    try {
        return JSON.stringify(value, null, 2)
    } catch {
        return String(value)
    }
}

function eventTone(event) {
    const type = String(event?.eventType || '').toUpperCase()
    if (event?.errorMessage || type.includes('FAIL') || type.includes('ERROR')) return 'failed'
    if (type.includes('WAIT')) return 'waiting'
    if (type.includes('SUCCESS') || type.includes('COMPLETE')) return 'success'
    return 'running'
}

function reportIsValid(report) {
    if (!report) return null
    if (typeof report.valid === 'boolean') return report.valid
    if (typeof report.success === 'boolean') return report.success
    if (Array.isArray(report.errors)) return report.errors.length === 0
    return null
}

function JsonDetails({label, value, defaultOpen = false}) {
    const [open, setOpen] = useState(defaultOpen)
    return (
        <details open={open} onToggle={(event) => setOpen(event.currentTarget.open)}>
            <summary>{label}</summary>
            {open && <pre>{pretty(value)}</pre>}
        </details>
    )
}

function FlowExecutionDock({
                               open,
                               onOpenChange,
                               payloadText,
                               onPayloadChange,
                               debugResult,
                               compileReport,
                               flow,
                               onRun,
                               busy,
                               readOnly,
                               selectedNodeId,
                               selectedEdgeIds = []
                           }) {
    const [tab, setTab] = useState('input')
    const [copied, setCopied] = useState(false)
    const events = debugResult?.events || []
    const selectedEvents = selectedNodeId ? events.filter((event) => event.nodeId === selectedNodeId) : events
    const visibleEvents = selectedEvents.slice(-200)
    const compileValid = reportIsValid(compileReport)
    const flowText = useMemo(() => pretty(flow), [flow])
    const payloadError = useMemo(() => {
        try {
            const parsed = JSON.parse(payloadText || '{}')
            return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? '' : '输入必须是 JSON 对象'
        } catch (error) {
            return error.message || 'JSON 格式不正确'
        }
    }, [payloadText])

    useEffect(() => {
        if (debugResult) setTab('execution')
    }, [debugResult])

    useEffect(() => {
        if (compileReport) setTab('compile')
    }, [compileReport])

    const copyDsl = async () => {
        try {
            await navigator.clipboard.writeText(flowText)
            setCopied(true)
            window.setTimeout(() => setCopied(false), 1400)
        } catch {
            setCopied(false)
        }
    }

    const tabs = [
        {id: 'input', label: '测试输入', icon: Code2},
        {id: 'execution', label: '执行结果', icon: TerminalSquare, count: events.length || null},
        {id: 'compile', label: '编译检查', icon: FileCheck2},
        {id: 'dsl', label: 'Flow DSL', icon: Braces}
    ]

    const handleTabKeyDown = (event) => {
        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
        event.preventDefault()
        let index = tabs.findIndex((item) => item.id === tab)
        if (event.key === 'Home') index = 0
        else if (event.key === 'End') index = tabs.length - 1
        else index = (index + (event.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length
        setTab(tabs[index].id)
        document.getElementById(`flow-execution-tab-${tabs[index].id}`)?.focus()
    }

    return (
        <section className={`flow-editor-execution-dock${open ? ' open' : ''}`} aria-label="流程执行面板">
            <header className="flow-editor-execution-bar">
                <button
                    type="button"
                    className="flow-editor-execution-toggle"
                    onClick={() => onOpenChange?.(!open)}
                    title={open ? '收起执行面板' : '展开执行面板'}
                    aria-label={open ? '收起执行面板' : '展开执行面板'}
                    aria-expanded={open}
                >
                    {open ? <ChevronDown size={16} aria-hidden="true"/> : <ChevronUp size={16} aria-hidden="true"/>}
                    <span>执行与调试</span>
                </button>
                <div className="flow-editor-execution-summary">
                    <span>{flow?.nodes?.length || 0} 个节点</span>
                    <span>{flow?.edges?.length || 0} 条连接</span>
                    {selectedNodeId && <span>已选节点 {selectedNodeId}</span>}
                    {!selectedNodeId && selectedEdgeIds.length > 0 && <span>已选 {selectedEdgeIds.length} 条连接</span>}
                </div>
                <button
                    type="button"
                    className="flow-editor-execution-run"
                    onClick={onRun}
                    disabled={Boolean(busy) || Boolean(readOnly) || Boolean(payloadError)}
                >
                    {busy === 'flow-debug'
                        ? <LoaderCircle className="flow-editor-spin" size={15} aria-hidden="true"/>
                        : <Play size={15} fill="currentColor" aria-hidden="true"/>}
                    测试运行
                </button>
            </header>

            {open && (
                <div className="flow-editor-execution-content">
                    <div
                        className="flow-editor-execution-tabs"
                        role="tablist"
                        aria-label="执行面板视图"
                        onKeyDown={handleTabKeyDown}
                    >
                        {tabs.map(({id, label, icon: Icon, count}) => (
                            <button
                                id={`flow-execution-tab-${id}`}
                                key={id}
                                type="button"
                                role="tab"
                                tabIndex={tab === id ? 0 : -1}
                                aria-selected={tab === id}
                                aria-controls="flow-execution-panel"
                                className={tab === id ? 'active' : ''}
                                onClick={() => setTab(id)}
                            >
                                <Icon size={14} aria-hidden="true"/>
                                <span>{label}</span>
                                {count != null && <b>{count}</b>}
                            </button>
                        ))}
                    </div>

                    <div
                        id="flow-execution-panel"
                        className="flow-editor-execution-view"
                        role="tabpanel"
                        aria-labelledby={`flow-execution-tab-${tab}`}
                    >
                        {tab === 'input' && (
                            <div className="flow-editor-execution-input">
                                <div className="flow-editor-execution-view-heading">
                                    <div>
                                        <strong>手动测试数据</strong>
                                        <span>JSON 对象</span>
                                    </div>
                                    {!payloadError &&
                                        <span className="flow-editor-execution-valid"><CheckCircle2 size={13}/> JSON 有效</span>}
                                </div>
                                <textarea
                                    value={payloadText}
                                    onChange={(event) => onPayloadChange?.(event.target.value)}
                                    spellCheck="false"
                                    aria-label="流程测试输入 JSON"
                                />
                                {payloadError && (
                                    <div className="flow-editor-execution-error">
                                        <AlertCircle size={14} aria-hidden="true"/>
                                        <span>{payloadError}</span>
                                    </div>
                                )}
                            </div>
                        )}

                        {tab === 'execution' && (
                            <div className="flow-editor-execution-results">
                                <div className="flow-editor-execution-view-heading">
                                    <div>
                                        <strong>{selectedNodeId ? `节点 ${selectedNodeId}` : '最近一次执行'}</strong>
                                        <span>{debugResult?.runId ? `Run ${debugResult.runId}` : '暂无运行记录'}</span>
                                    </div>
                                </div>
                                {selectedEvents.length ? (
                                    <div className="flow-editor-execution-events">
                                        {visibleEvents.map((event, index) => {
                                            const tone = eventTone(event)
                                            return (
                                                <article className="flow-editor-execution-event" data-tone={tone}
                                                         key={event.id || `${event.nodeId}-${index}`}>
                                                    <div className="flow-editor-execution-event-marker"><CircleDot
                                                        size={15} aria-hidden="true"/></div>
                                                    <div className="flow-editor-execution-event-main">
                                                        <header>
                                                            <div>
                                                                <strong>{event.nodeId || '流程事件'}</strong>
                                                                <span>{event.eventType || 'RUNNING'}</span>
                                                            </div>
                                                            <time>{event.elapsedMs == null ? '' : `${event.elapsedMs} ms`}</time>
                                                        </header>
                                                        {event.errorMessage && <p>{event.errorMessage}</p>}
                                                        {(event.input != null || event.output != null) && (
                                                            <div className="flow-editor-execution-io">
                                                                <JsonDetails label="输入" value={event.input}/>
                                                                <JsonDetails
                                                                    label="输出"
                                                                    value={event.output}
                                                                    defaultOpen={visibleEvents.length <= 12 && (tone === 'success' || tone === 'failed')}
                                                                />
                                                            </div>
                                                        )}
                                                    </div>
                                                </article>
                                            )
                                        })}
                                    </div>
                                ) : (
                                    <div className="flow-editor-execution-empty">
                                        <TerminalSquare size={22} aria-hidden="true"/>
                                        <strong>{selectedNodeId && events.length ? '该节点没有执行事件' : '还没有执行结果'}</strong>
                                        <span>0 条事件</span>
                                    </div>
                                )}
                            </div>
                        )}

                        {tab === 'compile' && (
                            <div className="flow-editor-execution-report">
                                <div className="flow-editor-execution-view-heading">
                                    <div>
                                        <strong>编译检查</strong>
                                        <span>校验节点、端口与运行时定义</span>
                                    </div>
                                    {compileValid != null && (
                                        <span
                                            className={compileValid ? 'flow-editor-execution-valid' : 'flow-editor-execution-invalid'}>
                              {compileValid ? <CheckCircle2 size={13}/> : <AlertCircle size={13}/>}
                                            {compileValid ? '检查通过' : '需要修正'}
                            </span>
                                    )}
                                </div>
                                {compileReport
                                    ? <pre>{pretty(compileReport)}</pre>
                                    : (
                                        <div className="flow-editor-execution-empty">
                                            <FileCheck2 size={22} aria-hidden="true"/>
                                            <strong>尚未执行编译检查</strong>
                                            <span>暂无报告</span>
                                        </div>
                                    )}
                            </div>
                        )}

                        {tab === 'dsl' && (
                            <div className="flow-editor-execution-dsl">
                                <div className="flow-editor-execution-view-heading">
                                    <div>
                                        <strong>HeartBeat Flow DSL</strong>
                                        <span>当前画布的领域模型快照</span>
                                    </div>
                                    <button type="button" onClick={copyDsl} title="复制 Flow DSL">
                                        {copied ? <CheckCircle2 size={14}/> : <Clipboard size={14}/>}
                                        {copied ? '已复制' : '复制'}
                                    </button>
                                </div>
                                <pre>{flowText}</pre>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </section>
    )
}

export default memo(FlowExecutionDock)
