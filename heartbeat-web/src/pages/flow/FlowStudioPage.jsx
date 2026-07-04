import {useEffect, useMemo, useState} from 'react'
import {flowApi} from '../../api'

const initialPayload = JSON.stringify({ status: 'PAID', orderNo: 'HB20260623001', amount: 99.9 }, null, 2)

function createInitialFlow() {
  return {
    name: '订单支付同步流程',
    code: `flow_${Date.now()}`,
    description: 'Open Flow Studio MVP 示例流程',
    status: 'DRAFT',
    variables: {},
    nodes: [
      {
        id: 'manual_1',
        type: 'trigger.manual',
        version: '1.0.0',
        position: { x: 120, y: 160 },
        config: {}
      }
    ],
    edges: [],
    settings: { timeoutMs: 30000 }
  }
}

function firstPort(component, direction) {
  const ports = component?.ports?.[direction] || []
  return ports[0]?.id || (direction === 'outputs' ? 'out' : 'in')
}

function JsonBlock({ value }) {
  return <pre className="flow-json-block">{JSON.stringify(value ?? {}, null, 2)}</pre>
}

export default function FlowStudioPage({ busy, onBusy, onError }) {
  const [components, setComponents] = useState([])
  const [flows, setFlows] = useState([])
  const [flow, setFlow] = useState(createInitialFlow)
  const [selectedNodeId, setSelectedNodeId] = useState('manual_1')
  const [nodeConfigText, setNodeConfigText] = useState('{}')
  const [payloadText, setPayloadText] = useState(initialPayload)
  const [compileReport, setCompileReport] = useState(null)
  const [debugResult, setDebugResult] = useState(null)

  useEffect(() => {
    let mounted = true
    Promise.all([flowApi.components(), flowApi.listFlows()])
        .then(([componentItems, flowItems]) => {
          if (!mounted) return
          setComponents(componentItems || [])
          setFlows(flowItems || [])
          if (flowItems?.[0]) {
            setFlow(flowItems[0])
            setSelectedNodeId(flowItems[0].nodes?.[0]?.id || '')
          }
        })
        .catch((error) => onError?.(error.message || '流程编排数据加载失败'))
    return () => {
      mounted = false
    }
  }, [onError])

  const componentByType = useMemo(() => {
    return components.reduce((map, component) => {
      map[component.type] = component
      return map
    }, {})
  }, [components])

  const groupedComponents = useMemo(() => {
    return components.reduce((groups, component) => {
      const category = component.category || '其他'
      groups[category] = groups[category] || []
      groups[category].push(component)
      return groups
    }, {})
  }, [components])

  const selectedNode = flow.nodes.find((node) => node.id === selectedNodeId)

  useEffect(() => {
    setNodeConfigText(JSON.stringify(selectedNode?.config || {}, null, 2))
  }, [selectedNodeId])

  function updateFlow(patch) {
    setFlow((current) => ({ ...current, ...patch }))
  }

  function addNode(component) {
    const index = flow.nodes.length + 1
    const node = {
      id: `${component.type.replace(/[^a-z0-9]/gi, '_')}_${index}`,
      type: component.type,
      version: component.version || '1.0.0',
      position: { x: 120 + index * 210, y: 150 + (index % 3) * 86 },
      config: {}
    }
    const edges = [...flow.edges]
    if (selectedNode) {
      const sourceComponent = componentByType[selectedNode.type]
      edges.push({
        id: `edge_${Date.now()}`,
        source: selectedNode.id,
        sourcePort: firstPort(sourceComponent, 'outputs'),
        target: node.id,
        targetPort: firstPort(component, 'inputs')
      })
    }
    setFlow({ ...flow, nodes: [...flow.nodes, node], edges })
    setSelectedNodeId(node.id)
  }

  function saveNodeConfig() {
    try {
      const config = JSON.parse(nodeConfigText || '{}')
      setFlow({
        ...flow,
        nodes: flow.nodes.map((node) => node.id === selectedNodeId ? { ...node, config } : node)
      })
      onError?.('')
    } catch (error) {
      onError?.(`节点配置不是合法 JSON：${error.message}`)
    }
  }

  async function run(action, work) {
    onBusy?.(action)
    try {
      return await work()
    } catch (error) {
      onError?.(error.message || '流程操作失败')
      return null
    } finally {
      onBusy?.('')
    }
  }

  async function saveDraft() {
    return run('flow-save', async () => {
      const saved = flow.id ? await flowApi.saveDraft(flow.id, flow) : await flowApi.createFlow(flow)
      setFlow(saved)
      setFlows((items) => [saved, ...items.filter((item) => item.id !== saved.id)])
      return saved
    })
  }

  async function compileFlow() {
    await run('flow-compile', async () => {
      const report = await flowApi.compile(flow.id, flow)
      setCompileReport(report)
    })
  }

  async function publishFlow() {
    if (!flow.id) {
      await saveDraft()
      return
    }
    await run('flow-publish', async () => {
      await flowApi.publish(flow.id)
      const items = await flowApi.listFlows()
      setFlows(items || [])
    })
  }

  async function debugFlow() {
    let flowForDebug = flow
    if (!flow.id) {
      flowForDebug = await saveDraft()
      if (!flowForDebug) return
    }
    await run('flow-debug', async () => {
      const payload = JSON.parse(payloadText || '{}')
      const result = await flowApi.debug(flowForDebug.id, payload)
      setDebugResult(result)
    })
  }

  return (
      <section className="flow-studio hb-page-card">
        <header className="module-page-header">
          <div>
            <p className="page-breadcrumb">数据配置 / Open Flow Studio</p>
            <h1>开放式流程编排</h1>
            <p>组件库开放注册，画布保存为 HeartBeat Flow DSL，并支持本地调试执行。</p>
          </div>
          <div className="module-page-meta">
            <span className="status-pill">{flow.status || 'DRAFT'}</span>
            <code>{flow.code}</code>
          </div>
        </header>

        <div className="flow-action-bar panel">
          <input value={flow.name || ''} onChange={(event) => updateFlow({ name: event.target.value })} aria-label="流程名称" />
          <button className="button ghost" disabled={Boolean(busy)} onClick={compileFlow}>编译校验</button>
          <button className="button ghost" disabled={Boolean(busy)} onClick={saveDraft}>保存草稿</button>
          <button className="button primary" disabled={Boolean(busy)} onClick={publishFlow}>发布版本</button>
        </div>

        <section className="flow-studio-grid">
          <aside className="flow-palette panel">
            <h2>组件库</h2>
            {Object.entries(groupedComponents).map(([category, items]) => (
                <div className="flow-palette-group" key={category}>
                  <strong>{category}</strong>
                  {items.map((component) => (
                      <button type="button" key={component.type} onClick={() => addNode(component)}>
                        <span>{component.icon || 'node'}</span>
                        <div>
                          <b>{component.name}</b>
                          <small>{component.type}</small>
                        </div>
                      </button>
                  ))}
                </div>
            ))}
          </aside>

          <main className="flow-canvas panel">
            <svg className="flow-edge-layer" viewBox="0 0 1200 640" preserveAspectRatio="none">
              {flow.edges.map((edge) => {
                const source = flow.nodes.find((node) => node.id === edge.source)
                const target = flow.nodes.find((node) => node.id === edge.target)
                if (!source || !target) return null
                const x1 = source.position.x + 160
                const y1 = source.position.y + 34
                const x2 = target.position.x
                const y2 = target.position.y + 34
                return (
                    <path
                        key={edge.id}
                        d={`M ${x1} ${y1} C ${x1 + 80} ${y1}, ${x2 - 80} ${y2}, ${x2} ${y2}`}
                        className="flow-edge-path"
                    />
                )
              })}
            </svg>
            {flow.nodes.map((node) => {
              const component = componentByType[node.type]
              const active = node.id === selectedNodeId
              return (
                  <button
                      key={node.id}
                      type="button"
                      className={active ? 'flow-node active' : 'flow-node'}
                      style={{ left: node.position.x, top: node.position.y }}
                      onClick={() => setSelectedNodeId(node.id)}
                  >
                    <span>{component?.category || 'Node'}</span>
                    <strong>{component?.name || node.type}</strong>
                    <small>{node.id}</small>
                  </button>
              )
            })}
          </main>

          <aside className="flow-inspector panel">
            <h2>节点属性</h2>
            {selectedNode ? (
                <>
                  <p><strong>{componentByType[selectedNode.type]?.name || selectedNode.type}</strong></p>
                  <small>{selectedNode.type}@{selectedNode.version}</small>
                  <textarea value={nodeConfigText} onChange={(event) => setNodeConfigText(event.target.value)} />
                  <button className="button secondary" onClick={saveNodeConfig}>应用配置</button>
                  <details>
                    <summary>组件 Manifest</summary>
                    <JsonBlock value={componentByType[selectedNode.type]} />
                  </details>
                </>
            ) : (
                <p className="muted">请选择一个节点。</p>
            )}
          </aside>
        </section>

        <section className="flow-debug-grid">
          <article className="panel">
            <div className="panel-heading">
              <h2>调试输入</h2>
              <button className="button primary" disabled={Boolean(busy)} onClick={debugFlow}>运行调试</button>
            </div>
            <textarea className="flow-debug-input" value={payloadText} onChange={(event) => setPayloadText(event.target.value)} />
          </article>
          <article className="panel">
            <h2>执行轨迹</h2>
            {debugResult ? (
                <div className="flow-run-events">
                  <strong>Run: {debugResult.runId}</strong>
                  {debugResult.events.map((event) => (
                      <div className="flow-run-event" key={event.id}>
                        <span>{event.nodeId}</span>
                        <b>{event.eventType}</b>
                        <small>{event.elapsedMs}ms</small>
                      </div>
                  ))}
                </div>
            ) : <p className="muted">运行调试后展示每个节点输入、输出和耗时。</p>}
          </article>
          <article className="panel">
            <h2>DSL / 编译报告</h2>
            {compileReport && <JsonBlock value={compileReport} />}
            <JsonBlock value={flow} />
          </article>
        </section>
      </section>
  )
}
