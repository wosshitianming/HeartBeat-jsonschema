import {memo} from 'react'
import {Handle, Position} from '@xyflow/react'
import {
    AlertTriangle,
    Box,
    CaseUpper,
    CheckCircle2,
    CircleStop,
    Clock3,
    Database,
    GitBranch,
    Globe2,
    LoaderCircle,
    Play,
    Plus,
    RadioTower,
    ScrollText,
    Shuffle,
    Webhook,
    XCircle,
    Zap
} from 'lucide-react'

const iconByName = {
    play: Play,
    webhook: Webhook,
    database: Database,
    redis: Database,
    mq: RadioTower,
    http: Globe2,
    condition: GitBranch,
    'case-upper': CaseUpper,
    mapper: Shuffle,
    log: ScrollText,
    end: CircleStop
}

function ComponentIcon({manifest, size = 18}) {
    const Icon = iconByName[String(manifest?.icon || '').toLowerCase()]
        || (manifest?.category === '触发器' ? Zap : Box)
    return <Icon size={size} aria-hidden="true"/>
}

function statusMeta(execution) {
    const status = typeof execution === 'string' ? execution : execution?.status
    if (status === 'success') return {label: '执行成功', icon: CheckCircle2}
    if (status === 'failed') return {label: '执行失败', icon: XCircle}
    if (status === 'waiting') return {label: '等待中', icon: Clock3}
    if (status === 'canceled') return {label: '已取消', icon: CircleStop}
    if (status === 'running') return {label: '执行中', icon: LoaderCircle}
    return null
}

function portOffset(index, count) {
    return `${Math.round(((index + 1) / (count + 1)) * 1000) / 10}%`
}

function FlowNodeCard({data = {}, selected, isConnectable = true}) {
    const node = data.node || {}
    const manifest = data.manifest || data.component
    const execution = data.executionState || data.executionStatus || data.debugStatus
    const readOnly = Boolean(data.readOnly)
    const inputs = manifest?.ports?.inputs || []
    const outputs = manifest?.ports?.outputs || []
    const state = statusMeta(execution)
    const StatusIcon = state?.icon
    const rowCount = Math.max(1, inputs.length, outputs.length)
    const nodeHeight = 76 + Math.max(0, rowCount - 2) * 20
    const nodeName = node.name || node.label || manifest?.name || node.type || '未知节点'
    const nodeCategory = manifest?.category || node.type || '未注册组件'
    const statusLabel = state
        ? `${state.label}${execution?.elapsedMs != null ? `，耗时 ${execution.elapsedMs} ms` : ''}`
        : ''

    return (
        <article
            className={`flow-editor-node-card${selected ? ' selected' : ''}`}
            data-execution={typeof execution === 'string' ? execution : execution?.status || ''}
            aria-label={nodeName}
            style={{
                '--flow-node-accent': data.accent || '#596579',
                '--flow-node-height': `${nodeHeight}px`
            }}
        >
            <header className="flow-editor-node-header">
                <div className="flow-editor-node-icon">
                    <ComponentIcon manifest={manifest}/>
                </div>
                <div className="flow-editor-node-title">
                    <strong title={nodeName}>{nodeName}</strong>
                    <span title={`${nodeCategory} · ${node.type || 'unknown'}`}>{nodeCategory}</span>
                </div>
                {state ? (
                    <span className="flow-editor-node-status" title={statusLabel} aria-label={statusLabel}>
                <StatusIcon
                    className={(typeof execution === 'string' ? execution : execution?.status) === 'running'
                        ? 'flow-editor-spin'
                        : ''}
                    size={16}
                    aria-hidden="true"
                />
              </span>
                ) : !manifest ? (
                    <span className="flow-editor-node-status unknown" title="组件版本不可用"
                          aria-label="组件版本不可用">
                <AlertTriangle size={16} aria-hidden="true"/>
              </span>
                ) : null}
            </header>

            <div className="flow-editor-node-ports">
                {inputs.map((port, index) => (
                    <div
                        className="flow-editor-node-port input"
                        data-label={`${port.label || port.id}${port.required ? ' *' : ''}`}
                        key={port.id}
                        style={{'--flow-port-offset': portOffset(index, inputs.length)}}
                        title={`${port.label || port.id} · ${port.schema || 'any'}`}
                    >
                        <Handle
                            id={port.id}
                            type="target"
                            position={Position.Left}
                            isConnectable={isConnectable && !readOnly}
                            aria-label={`输入端口 ${port.label || port.id}${port.required ? '，必需' : ''}`}
                        />
                    </div>
                ))}
                {outputs.map((port, index) => (
                    <div
                        className="flow-editor-node-port output"
                        data-label={port.label || port.id}
                        data-port={port.id}
                        key={port.id}
                        style={{'--flow-port-offset': portOffset(index, outputs.length)}}
                        title={`${port.label || port.id} · ${port.schema || 'any'}`}
                    >
                        <Handle
                            id={port.id}
                            type="source"
                            position={Position.Right}
                            isConnectable={isConnectable && !readOnly}
                            aria-label={`输出端口 ${port.label || port.id}`}
                        />
                        {!readOnly && (
                            <button
                                type="button"
                                className="flow-editor-node-quick-add nodrag nopan"
                                onClick={(event) => {
                                    event.stopPropagation()
                                    data.onQuickAdd?.(node.id, port.id)
                                }}
                                title={`从“${port.label || port.id}”添加后续节点`}
                                aria-label={`从“${port.label || port.id}”添加后续节点`}
                            >
                                <Plus size={12} aria-hidden="true"/>
                            </button>
                        )}
                    </div>
                ))}
            </div>
        </article>
    )
}

export default memo(FlowNodeCard)
