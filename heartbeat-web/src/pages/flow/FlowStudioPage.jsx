import {useCallback, useEffect, useMemo, useRef, useState} from 'react'
import {reconnectEdge} from '@xyflow/react'
import {FilePlus2, PanelLeftOpen, SlidersHorizontal} from 'lucide-react'
import {useLocation} from 'react-router-dom'
import '@xyflow/react/dist/style.css'
import {flowApi} from '../../api'
import {hasPermission} from '../../domain/admin/permissionPolicy'
import FlowEditorToolbar from '../../components/flow/FlowEditorToolbar'
import FlowExecutionDock from '../../components/flow/FlowExecutionDock'
import NodeInspector from '../../components/flow/NodeInspector'
import NodeLibrary from '../../components/flow/NodeLibrary'
import FlowCanvas from './studio/FlowCanvas'
import {
    buildManifestIndex,
    canvasToFlowDsl,
    createStableId,
    decorateCanvasNodes,
    flowDslToCanvas,
    resolveManifest,
    validateConnection
} from './flowCanvasAdapter'
import './FlowStudioPage.css'

const initialPayload = JSON.stringify({status: 'PAID', orderNo: 'HB20260712001', amount: 99.9}, null, 2)

function createInitialFlow() {
  return {
      name: '新建自动化流程',
    code: `flow_${Date.now()}`,
      description: '',
    status: 'DRAFT',
    variables: {},
      nodes: [],
    edges: [],
      settings: {timeoutMs: 30000, editor: {}}
  }
}

function normalizeFlow(value) {
    const fallback = createInitialFlow()
    return {
        ...fallback,
        ...(value || {}),
        variables: value?.variables || {},
        nodes: Array.isArray(value?.nodes) ? value.nodes : [],
        edges: Array.isArray(value?.edges) ? value.edges : [],
        settings: {...fallback.settings, ...(value?.settings || {})}
    }
}

function cloneDocument(value) {
    return JSON.parse(JSON.stringify(value))
}

function editableTarget(target) {
    if (!target) return false
    const tag = target.tagName?.toLowerCase()
    return target.isContentEditable || tag === 'input' || tag === 'textarea' || tag === 'select'
}

function compactCanvasLayout() {
    return typeof window !== 'undefined' && window.matchMedia?.('(max-width: 620px)').matches
}

function singlePanelCanvasLayout() {
    return typeof window !== 'undefined' && window.matchMedia?.('(max-width: 1450px)').matches
}

function businessDocumentSignature(value) {
    const editor = {...(value?.settings?.editor || {})}
    delete editor.viewport
    return JSON.stringify({
        ...(value || {}),
        settings: {...(value?.settings || {}), editor}
    })
}

function schemaDefaults(schema) {
    if (!schema || typeof schema !== 'object') return {}
    if (schema.default !== undefined) return cloneDocument(schema.default)
    if (schema.type !== 'object' && !schema.properties) return {}
    return Object.entries(schema.properties || {}).reduce((result, [key, property]) => {
        if (property?.default !== undefined) result[key] = cloneDocument(property.default)
        return result
    }, {})
}

function executionState(events = []) {
    const states = new Map()
    for (const event of events) {
        if (!event?.nodeId || event.nodeId === '__process__') continue
        const type = String(event.eventType || '').toUpperCase()
        let status = 'running'
        if (type.includes('FAIL') || type.includes('ERROR') || event.errorMessage) status = 'failed'
        else if (type.includes('WAIT')) status = 'waiting'
        else if (type.includes('SUCCESS') || type.includes('COMPLETE')) status = 'success'
        else if (type.includes('CANCEL')) status = 'canceled'
        states.set(event.nodeId, {
            status,
            elapsedMs: event.elapsedMs,
            eventType: event.eventType,
            errorMessage: event.errorMessage,
            input: event.input,
            output: event.output
        })
    }
    return states
}

export default function FlowStudioPage({permissions = [], busy, onBusy, onError}) {
    const location = useLocation()
    const requestedFlowId = useMemo(
        () => new URLSearchParams(location.search).get('flowId')?.trim() || '',
        [location.search]
    )
    const canEdit = hasPermission(permissions, 'flow:definition:edit')
    const canPublish = hasPermission(permissions, 'flow:definition:publish')
  const [components, setComponents] = useState([])
  const [flows, setFlows] = useState([])
  const [flow, setFlow] = useState(createInitialFlow)
    const [canvasNodes, setCanvasNodes] = useState([])
    const [canvasEdges, setCanvasEdges] = useState([])
    const [selectedNodeId, setSelectedNodeId] = useState('')
    const [selectedEdgeIds, setSelectedEdgeIds] = useState([])
    const [inspectorValue, setInspectorValue] = useState({})
  const [payloadText, setPayloadText] = useState(initialPayload)
  const [compileReport, setCompileReport] = useState(null)
  const [debugResult, setDebugResult] = useState(null)
    const [libraryOpen, setLibraryOpen] = useState(true)
    const [inspectorOpen, setInspectorOpen] = useState(true)
    const [dockOpen, setDockOpen] = useState(false)
    const [mobileCanvasLayout, setMobileCanvasLayout] = useState(compactCanvasLayout)
    const [singlePanelLayout, setSinglePanelLayout] = useState(singlePanelCanvasLayout)
    const [dirty, setDirty] = useState(false)
    const [historyVersion, setHistoryVersion] = useState(0)
    const [pendingConnection, setPendingConnection] = useState(null)

    const flowRef = useRef(flow)
    const graphRef = useRef({nodes: [], edges: []})
    const manifestIndexRef = useRef(buildManifestIndex([]))
    const savedSnapshotRef = useRef(businessDocumentSignature(flow))
    const historyRef = useRef({past: [], future: []})
    const clipboardRef = useRef(null)
    const pasteOffsetRef = useRef(0)
    const viewportRef = useRef(null)
    const reactFlowRef = useRef(null)
    const selectedNodeIdRef = useRef('')
    const inspectorValueRef = useRef({})
    const inspectorDirtyRef = useRef(false)
    const displayNodeCacheRef = useRef(new Map())

    const manifestIndex = useMemo(() => buildManifestIndex(components), [components])
    const debugByNode = useMemo(() => executionState(debugResult?.events || []), [debugResult])
    const payloadIsValid = useMemo(() => {
        try {
            const value = JSON.parse(payloadText || '{}')
            return Boolean(value && typeof value === 'object' && !Array.isArray(value))
        } catch {
            return false
        }
    }, [payloadText])

    useEffect(() => {
        manifestIndexRef.current = manifestIndex
    }, [manifestIndex])

    useEffect(() => {
        const updateLayout = () => {
            setMobileCanvasLayout(compactCanvasLayout())
            setSinglePanelLayout(singlePanelCanvasLayout())
        }
        updateLayout()
        window.addEventListener('resize', updateLayout)
        return () => window.removeEventListener('resize', updateLayout)
    }, [])

    const setGraph = useCallback((nodes, edges) => {
        graphRef.current = {nodes, edges}
        setCanvasNodes(nodes)
        setCanvasEdges(edges)
    }, [])

    const invalidateDerivedResults = useCallback(() => {
        setCompileReport(null)
        setDebugResult(null)
    }, [])

    const invalidateRedo = useCallback(() => {
        if (!historyRef.current.future.length) return
        historyRef.current.future = []
        setHistoryVersion((value) => value + 1)
    }, [])

    const createGraph = useCallback((document, index = manifestIndexRef.current, selectedIds = []) => {
        const graph = flowDslToCanvas(document, index)
        const selected = new Set(selectedIds)
        return {
            nodes: (graph.nodes || []).map((node) => ({...node, selected: selected.has(node.id)})),
            edges: graph.edges || []
        }
    }, [])

    const updateDirty = useCallback((document) => {
        setDirty(businessDocumentSignature(document) !== savedSnapshotRef.current)
    }, [])

    const applyDocument = useCallback((document, options = {}) => {
        const normalized = normalizeFlow(document)
        const selectedIds = options.selectedIds || []
        if (options.record !== false) {
            const current = cloneDocument(flowRef.current)
            const past = historyRef.current.past
            if (!past.length || JSON.stringify(past[past.length - 1]) !== JSON.stringify(current)) {
                past.push(current)
                if (past.length > 50) past.shift()
            }
            historyRef.current.future = []
            setHistoryVersion((value) => value + 1)
        }
        flowRef.current = normalized
        setFlow(normalized)
        const graph = createGraph(normalized, options.index || manifestIndexRef.current, selectedIds)
        setGraph(graph.nodes, graph.edges)
        const primary = selectedIds[0] || ''
        selectedNodeIdRef.current = primary
        inspectorDirtyRef.current = false
        inspectorValueRef.current = cloneDocument(normalized.nodes.find((node) => node.id === primary)?.config || {})
        setInspectorValue(inspectorValueRef.current)
        setSelectedNodeId(primary)
        setSelectedEdgeIds([])
        invalidateDerivedResults()
        updateDirty(normalized)
        return normalized
    }, [createGraph, invalidateDerivedResults, setGraph, updateDirty])

    const loadDocument = useCallback((document, index = manifestIndexRef.current) => {
        const normalized = normalizeFlow(document)
        flowRef.current = normalized
        setFlow(normalized)
        const graph = createGraph(normalized, index)
        setGraph(graph.nodes, graph.edges)
        historyRef.current = {past: [], future: []}
        setHistoryVersion((value) => value + 1)
        savedSnapshotRef.current = businessDocumentSignature(normalized)
        setDirty(false)
        selectedNodeIdRef.current = ''
        inspectorValueRef.current = {}
        inspectorDirtyRef.current = false
        setInspectorValue({})
        setSelectedNodeId('')
        setSelectedEdgeIds([])
        setCompileReport(null)
        setDebugResult(null)
        setPendingConnection(null)
        viewportRef.current = normalized.settings?.editor?.viewport || null
        requestAnimationFrame(() => {
            if (viewportRef.current) reactFlowRef.current?.setViewport(viewportRef.current, {duration: 180})
            else reactFlowRef.current?.fitView({padding: 0.22, duration: 220, maxZoom: 1.1})
        })
    }, [createGraph, setGraph])

  useEffect(() => {
    let mounted = true
    Promise.all([flowApi.components(), flowApi.listFlows()])
        .then(async ([componentItems, flowItems]) => {
          if (!mounted) return
            const manifests = componentItems || []
            const items = flowItems || []
            const targetFlowId = requestedFlowId || items[0]?.id
            const document = targetFlowId ? await flowApi.flow(targetFlowId) : createInitialFlow()
            if (!mounted) return
            setComponents(manifests)
            setFlows(items)
            const index = buildManifestIndex(manifests)
            manifestIndexRef.current = index
            loadDocument(document, index)
        })
        .catch((error) => onError?.(error.message || '流程编排数据加载失败'))
    return () => {
      mounted = false
    }
  }, [loadDocument, onError, requestedFlowId])

  useEffect(() => {
      const warnBeforeLeave = (event) => {
          if (!dirty) return
          event.preventDefault()
          event.returnValue = ''
      }
      window.addEventListener('beforeunload', warnBeforeLeave)
      return () => window.removeEventListener('beforeunload', warnBeforeLeave)
  }, [dirty])

    useEffect(() => {
        const graph = createGraph(flowRef.current, manifestIndex)
        const selectedIds = graphRef.current.nodes.filter((node) => node.selected).map((node) => node.id)
        graph.nodes = graph.nodes.map((node) => ({...node, selected: selectedIds.includes(node.id)}))
        setGraph(graph.nodes, graph.edges)
    }, [createGraph, manifestIndex, setGraph])

    const currentDocument = useCallback(() => {
        let document = canvasToFlowDsl(flowRef.current, graphRef.current.nodes, graphRef.current.edges)
        if (inspectorDirtyRef.current && selectedNodeIdRef.current) {
            document = {
                ...document,
                nodes: document.nodes.map((node) => node.id === selectedNodeIdRef.current
                    ? {...node, config: cloneDocument(inspectorValueRef.current)}
                    : node)
            }
        }
        const viewport = viewportRef.current
        if (!viewport) return document
        return {
            ...document,
            settings: {
                ...(document.settings || {}),
                editor: {...(document.settings?.editor || {}), viewport}
            }
    }
    }, [])

    const commitGraph = useCallback((selectedIds = []) => {
        const document = currentDocument()
        return applyDocument(document, {selectedIds})
    }, [applyDocument, currentDocument])

    const flushInspectorConfig = useCallback((selectedIds = []) => {
        if (!inspectorDirtyRef.current || !selectedNodeIdRef.current) return false
        applyDocument(currentDocument(), {selectedIds})
        return true
    }, [applyDocument, currentDocument])

    const selectedNode = useMemo(
        () => flow.nodes.find((node) => node.id === selectedNodeId) || null,
        [flow.nodes, selectedNodeId]
    )
    const selectedManifest = useMemo(
        () => selectedNode ? resolveManifest(manifestIndex, selectedNode.type, selectedNode.version) : null,
        [manifestIndex, selectedNode]
    )

    useEffect(() => {
        const nextValue = cloneDocument(selectedNode?.config || {})
        inspectorValueRef.current = nextValue
        inspectorDirtyRef.current = false
        setInspectorValue(nextValue)
    }, [selectedNodeId, selectedNode?.config])

    useEffect(() => {
        if (!singlePanelLayout || !libraryOpen || !inspectorOpen) return
        if (selectedNodeIdRef.current) setLibraryOpen(false)
        else setInspectorOpen(false)
    }, [inspectorOpen, libraryOpen, singlePanelLayout])

    useEffect(() => {
        if (!mobileCanvasLayout) return
        if (dockOpen) {
            setLibraryOpen(false)
            setInspectorOpen(false)
            return
        }
        if (flow.id || !canEdit) setLibraryOpen(false)
    }, [canEdit, dockOpen, flow.id, mobileCanvasLayout])

    const handleInspectorValueChange = useCallback((nextValue) => {
        const normalized = cloneDocument(nextValue || {})
        inspectorValueRef.current = normalized
        inspectorDirtyRef.current = JSON.stringify(normalized) !== JSON.stringify(selectedNode?.config || {})
        setInspectorValue(normalized)
        invalidateRedo()
        invalidateDerivedResults()
        updateDirty(currentDocument())
    }, [currentDocument, invalidateDerivedResults, invalidateRedo, selectedNode?.config, updateDirty])

    const openLibrary = useCallback(() => {
        if (singlePanelLayout) setInspectorOpen(false)
        if (mobileCanvasLayout) setDockOpen(false)
        setLibraryOpen(true)
    }, [mobileCanvasLayout, singlePanelLayout])

    const openInspector = useCallback(() => {
        if (singlePanelLayout) setLibraryOpen(false)
        if (mobileCanvasLayout) setDockOpen(false)
        setInspectorOpen(true)
    }, [mobileCanvasLayout, singlePanelLayout])

    const handleDockOpenChange = useCallback((nextOpen) => {
        if (nextOpen && mobileCanvasLayout) {
            setLibraryOpen(false)
            setInspectorOpen(false)
    }
        setDockOpen(nextOpen)
    }, [mobileCanvasLayout])

    const handleQuickAdd = useCallback((nodeId, sourcePort) => {
        const compatibleComponents = components.filter((component) => (component?.ports?.inputs || []).length > 0)
        if (!compatibleComponents.length) {
            onError?.('当前没有可连接的后续节点组件')
            return
        }
        setPendingConnection({source: nodeId, sourceHandle: sourcePort})
        openLibrary()
    }, [components, onError, openLibrary])

    const libraryComponents = useMemo(
        () => pendingConnection
            ? components.filter((component) => (component?.ports?.inputs || []).length > 0)
            : components,
        [components, pendingConnection]
    )

    const displayNodes = useMemo(() => {
        const decorated = decorateCanvasNodes(canvasNodes, {
            manifestIndex,
            debugByNode,
            canEdit,
            onQuickAdd: handleQuickAdd
        }, displayNodeCacheRef.current)
        displayNodeCacheRef.current = decorated.cache
        return decorated.nodes
    }, [canvasNodes, canEdit, debugByNode, handleQuickAdd, manifestIndex])

    const runAction = useCallback(async (action, work) => {
    onBusy?.(action)
    try {
        const result = await work()
        onError?.('')
        return result
    } catch (error) {
      onError?.(error.message || '流程操作失败')
      return null
    } finally {
      onBusy?.('')
    }
    }, [onBusy, onError])

    const persistDraft = useCallback(async () => {
        const document = currentDocument()
        const saved = document.id
            ? await flowApi.saveDraft(document.id, document)
            : await flowApi.createFlow(document)
        const normalized = normalizeFlow(saved)
        flowRef.current = normalized
        setFlow(normalized)
        const selectedIds = graphRef.current.nodes.filter((node) => node.selected).map((node) => node.id)
        const graph = createGraph(normalized, manifestIndexRef.current, selectedIds)
        setGraph(graph.nodes, graph.edges)
        setFlows((items) => [normalized, ...items.filter((item) => item.id !== normalized.id)])
        savedSnapshotRef.current = businessDocumentSignature(normalized)
        inspectorDirtyRef.current = false
        setDirty(false)
        return normalized
    }, [createGraph, currentDocument, setGraph])

    const saveDraft = useCallback(() => runAction('flow-save', persistDraft), [persistDraft, runAction])

    const compileFlow = useCallback(() => runAction('flow-compile', async () => {
        const document = currentDocument()
        setCompileReport(null)
        const report = await flowApi.compile(document.id, document)
        setCompileReport(report)
        handleDockOpenChange(true)
        return report
    }), [currentDocument, handleDockOpenChange, runAction])

    const publishFlow = useCallback(() => runAction('flow-publish', async () => {
        let document = currentDocument()
        if (canEdit) document = await persistDraft()
        if (!document?.id) throw new Error('请先保存流程草稿')
        await flowApi.publish(document.id)
        const items = await flowApi.listFlows()
        setFlows(items || [])
        const published = (items || []).find((item) => item.id === document.id)
            || await flowApi.flow(document.id)
        loadDocument(published, manifestIndexRef.current)
        return published
    }), [canEdit, currentDocument, loadDocument, persistDraft, runAction])

    const debugFlow = useCallback(() => runAction('flow-debug', async () => {
        if (!canEdit) throw new Error('当前账号没有流程调试权限')
        setDebugResult(null)
        let document = currentDocument()
        if (canEdit) document = await persistDraft()
        if (!document?.id) throw new Error('请先保存流程草稿')
        const payload = JSON.parse(payloadText || '{}')
        if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
            throw new Error('调试输入必须是 JSON 对象')
        }
        const result = await flowApi.debug(document.id, payload)
        setDebugResult(result)
        handleDockOpenChange(true)
        return result
    }), [canEdit, currentDocument, handleDockOpenChange, payloadText, persistDraft, runAction])

    const normalizeConnection = useCallback((connection) => {
        const sourceNode = graphRef.current.nodes.find((node) => node.id === connection?.source)
        const targetNode = graphRef.current.nodes.find((node) => node.id === connection?.target)
        const sourceDomain = sourceNode?.data?.node
        const targetDomain = targetNode?.data?.node
        const sourceManifest = resolveManifest(manifestIndexRef.current, sourceDomain?.type, sourceDomain?.version)
        const targetManifest = resolveManifest(manifestIndexRef.current, targetDomain?.type, targetDomain?.version)
        return {
            ...connection,
            sourceHandle: connection?.sourceHandle || sourceManifest?.ports?.outputs?.[0]?.id || null,
            targetHandle: connection?.targetHandle || targetManifest?.ports?.inputs?.[0]?.id || null
        }
    }, [])

    const connectionIsValid = useCallback((connection) => {
        const result = validateConnection(
            normalizeConnection(connection),
            graphRef.current.nodes,
            graphRef.current.edges,
            manifestIndexRef.current
        )
        return typeof result === 'boolean' ? result : result?.valid !== false
    }, [normalizeConnection])

    const handleConnect = useCallback((connection) => {
        const normalizedConnection = normalizeConnection(connection)
        if (!canEdit || !connectionIsValid(normalizedConnection)) return
        const edge = {
            id: createStableId('edge'),
            source: normalizedConnection.source,
            target: normalizedConnection.target,
            sourceHandle: normalizedConnection.sourceHandle,
            targetHandle: normalizedConnection.targetHandle,
            type: 'smoothstep'
        }
        graphRef.current = {...graphRef.current, edges: [...graphRef.current.edges, edge]}
        setCanvasEdges(graphRef.current.edges)
        commitGraph([normalizedConnection.target])
    }, [canEdit, commitGraph, connectionIsValid, normalizeConnection])

    const handleReconnect = useCallback((oldEdge, connection) => {
        const normalizedConnection = normalizeConnection({...connection, replacingEdgeId: oldEdge.id})
        if (!canEdit || !connectionIsValid(normalizedConnection)) return
        const edges = reconnectEdge(oldEdge, normalizedConnection, graphRef.current.edges)
        graphRef.current = {...graphRef.current, edges}
        setCanvasEdges(edges)
        commitGraph([normalizedConnection.target])
    }, [canEdit, commitGraph, connectionIsValid, normalizeConnection])

    const handleNodeDragStop = useCallback((event, node, draggedNodes = []) => {
        if (!canEdit) return
        const movedNodes = draggedNodes.length ? draggedNodes : [node]
        const movedById = new Map(movedNodes.map((item) => [item.id, item]))
        let positionChanged = false
        const nodes = graphRef.current.nodes.map((item) => {
            const moved = movedById.get(item.id)
            if (!moved) return item
            const nextPosition = {...moved.position}
            if (item.position?.x === nextPosition.x && item.position?.y === nextPosition.y) return item
            positionChanged = true
            return {
                ...item,
                position: nextPosition,
                selected: moved.selected ?? item.selected,
                data: item.data?.node
                    ? {...item.data, node: {...item.data.node, position: nextPosition}}
                    : item.data
            }
        })
        if (!positionChanged) return
        graphRef.current = {...graphRef.current, nodes}
        const document = normalizeFlow(currentDocument())
        const past = historyRef.current.past
        past.push(flowRef.current)
        if (past.length > 50) past.shift()
        historyRef.current.future = []
        setHistoryVersion((value) => value + 1)
        flowRef.current = document
        setFlow(document)
        setCanvasNodes(nodes)
        inspectorDirtyRef.current = false
        invalidateDerivedResults()
        updateDirty(document)
    }, [canEdit, currentDocument, invalidateDerivedResults, updateDirty])

    const handleDelete = useCallback(({nodes: deletedNodes = [], edges: deletedEdges = []}) => {
        if (!canEdit) return
        const nodeIds = new Set(deletedNodes.map((node) => node.id))
        const edgeIds = new Set(deletedEdges.map((edge) => edge.id))
        const nodes = graphRef.current.nodes.filter((node) => !nodeIds.has(node.id))
        const edges = graphRef.current.edges.filter((edge) => !edgeIds.has(edge.id)
            && !nodeIds.has(edge.source)
            && !nodeIds.has(edge.target))
        setGraph(nodes, edges)
        const selectedIds = selectedNodeId && !nodeIds.has(selectedNodeId) ? [selectedNodeId] : []
        commitGraph(selectedIds)
    }, [canEdit, commitGraph, selectedNodeId, setGraph])

    const handleSelectionChange = useCallback(({nodes, edges}) => {
        const nextSelectedNodeIds = (nodes || []).map((node) => node.id)
        const selectedNodeIds = new Set(nextSelectedNodeIds)
        const selectedEdgeIds = new Set((edges || []).map((edge) => edge.id))
        const nextNodeId = nextSelectedNodeIds[0] || ''
        if (nextNodeId !== selectedNodeIdRef.current) flushInspectorConfig(nextSelectedNodeIds)
        graphRef.current = {
            nodes: graphRef.current.nodes.map((node) => node.selected === selectedNodeIds.has(node.id)
                ? node
                : {...node, selected: selectedNodeIds.has(node.id)}),
            edges: graphRef.current.edges.map((edge) => edge.selected === selectedEdgeIds.has(edge.id)
                ? edge
                : {...edge, selected: selectedEdgeIds.has(edge.id)})
        }
        selectedNodeIdRef.current = nextNodeId
        setSelectedNodeId(nextNodeId)
        setSelectedEdgeIds([...selectedEdgeIds])
    }, [flushInspectorConfig])

    const handleCanvasNodeClick = useCallback((event, node) => {
        if (node.id !== selectedNodeIdRef.current) flushInspectorConfig([node.id])
        selectedNodeIdRef.current = node.id
        setSelectedNodeId(node.id)
        openInspector()
    }, [flushInspectorConfig, openInspector])

    const clearSelection = useCallback(() => {
        flushInspectorConfig()
        const nodes = graphRef.current.nodes.map((node) => ({...node, selected: false}))
        const edges = graphRef.current.edges.map((edge) => ({...edge, selected: false}))
        setGraph(nodes, edges)
        selectedNodeIdRef.current = ''
        setSelectedNodeId('')
        setSelectedEdgeIds([])
    }, [flushInspectorConfig, setGraph])

    const addNode = useCallback((componentOrType, position) => {
        if (!canEdit) return
        flushInspectorConfig()
        const componentRef = typeof componentOrType === 'string'
            ? {type: componentOrType}
            : componentOrType
        const component = components.find((item) => item.type === componentRef?.type
                && (!componentRef?.version || item.version === componentRef.version))
            || (componentRef?.ports ? componentRef : null)
        if (!component) return
        if (pendingConnection && !(component.ports?.inputs || []).length) {
            onError?.('该组件没有输入端口，无法作为后续节点')
            return
        }
        const sourceNode = pendingConnection
            ? flowRef.current.nodes.find((node) => node.id === pendingConnection.source)
            : null
        const index = flowRef.current.nodes.length
        const resolvedPosition = position || (sourceNode
            ? {x: sourceNode.position.x + 256, y: sourceNode.position.y}
            : {x: 112 + (index % 4) * 256, y: 96 + Math.floor(index / 4) * 128})
        const node = {
            id: createStableId(component.type.split('.').pop() || 'node'),
            type: component.type,
            version: component.version || '1.0.0',
            position: {x: Math.round(resolvedPosition.x), y: Math.round(resolvedPosition.y)},
            config: schemaDefaults(component.configSchema)
        }
        const next = cloneDocument(flowRef.current)
        next.nodes.push(node)
        if (pendingConnection && sourceNode) {
            const input = component.ports?.inputs?.[0]
            if (input) {
                next.edges.push({
                    id: createStableId('edge'),
                    source: pendingConnection.source,
                    sourcePort: pendingConnection.sourceHandle,
                    target: node.id,
                    targetPort: input.id
                })
            }
        }
        applyDocument(next, {selectedIds: [node.id]})
        setPendingConnection(null)
        openInspector()
    }, [applyDocument, canEdit, components, flushInspectorConfig, onError, openInspector, pendingConnection])

    const handleLibraryDragStart = useCallback((event, component) => {
        event.dataTransfer.effectAllowed = 'copy'
        event.dataTransfer.setData('application/heartbeat-flow-component', JSON.stringify({
            type: component.type,
            version: component.version
        }))
    }, [])

    const applyNodeConfig = useCallback((value) => {
        if (!selectedNode || !canEdit) return
        const nextConfig = value ?? inspectorValueRef.current
        const next = cloneDocument(flowRef.current)
        next.nodes = next.nodes.map((node) => node.id === selectedNode.id
            ? {...node, config: cloneDocument(nextConfig || {})}
            : node)
        applyDocument(next, {selectedIds: [selectedNode.id]})
    }, [applyDocument, canEdit, selectedNode])

    const updateFlowName = useCallback((name) => {
        const next = {...flowRef.current, name}
        flowRef.current = next
        setFlow(next)
        invalidateRedo()
        invalidateDerivedResults()
        updateDirty(currentDocument())
    }, [currentDocument, invalidateDerivedResults, invalidateRedo, updateDirty])

    const undo = useCallback(() => {
        flushInspectorConfig([selectedNodeIdRef.current].filter(Boolean))
        const previous = historyRef.current.past.pop()
        if (!previous) return
        historyRef.current.future.unshift(cloneDocument(flowRef.current))
        applyDocument(previous, {record: false})
        setHistoryVersion((value) => value + 1)
    }, [applyDocument, flushInspectorConfig])

    const redo = useCallback(() => {
        const next = historyRef.current.future.shift()
        if (!next) return
        historyRef.current.past.push(cloneDocument(flowRef.current))
        applyDocument(next, {record: false})
        setHistoryVersion((value) => value + 1)
    }, [applyDocument])

    const selectedSnapshot = useCallback(() => {
        const document = currentDocument()
        const selectedIds = new Set(graphRef.current.nodes.filter((node) => node.selected).map((node) => node.id))
        if (!selectedIds.size && selectedNodeId) selectedIds.add(selectedNodeId)
        return {
            nodes: document.nodes.filter((node) => selectedIds.has(node.id)),
            edges: document.edges.filter((edge) => selectedIds.has(edge.source) && selectedIds.has(edge.target))
        }
    }, [currentDocument, selectedNodeId])

    const pasteSnapshot = useCallback((snapshot) => {
        if (!canEdit || !snapshot?.nodes?.length) return
        pasteOffsetRef.current = (pasteOffsetRef.current % 5) + 1
        const offset = pasteOffsetRef.current * 32
        const idMap = new Map()
        const nodes = snapshot.nodes.map((node) => {
            const id = createStableId(node.type.split('.').pop() || 'node')
            idMap.set(node.id, id)
            return {...cloneDocument(node), id, position: {x: node.position.x + offset, y: node.position.y + offset}}
    })
        const edges = snapshot.edges.map((edge) => ({
            ...cloneDocument(edge),
            id: createStableId('edge'),
            source: idMap.get(edge.source),
            target: idMap.get(edge.target)
        }))
        const next = currentDocument()
        next.nodes = [...next.nodes, ...nodes]
        next.edges = [...next.edges, ...edges]
        applyDocument(next, {selectedIds: nodes.map((node) => node.id)})
    }, [applyDocument, canEdit, currentDocument])

    const selectAll = useCallback(() => {
        const selectedIds = graphRef.current.nodes.map((node) => node.id)
        flushInspectorConfig(selectedIds)
        const nodes = graphRef.current.nodes.map((node) => ({...node, selected: true}))
        setGraph(nodes, graphRef.current.edges)
        selectedNodeIdRef.current = nodes[0]?.id || ''
        setSelectedNodeId(selectedNodeIdRef.current)
    }, [flushInspectorConfig, setGraph])

    useEffect(() => {
        const handleKeyDown = (event) => {
            const command = event.metaKey || event.ctrlKey
            const key = event.key.toLowerCase()
            if (command && key === 's') {
                event.preventDefault()
                if (canEdit && !busy) saveDraft()
                return
            }
            if (editableTarget(event.target)) return
            if (command && key === 'z') {
                event.preventDefault()
                if (event.shiftKey) redo()
                else undo()
            } else if (command && key === 'y') {
                event.preventDefault()
                redo()
            } else if (command && key === 'a') {
                event.preventDefault()
                selectAll()
            } else if (command && key === 'c') {
                const snapshot = selectedSnapshot()
                if (snapshot.nodes.length) clipboardRef.current = snapshot
            } else if (command && key === 'v') {
                event.preventDefault()
                pasteSnapshot(clipboardRef.current)
            } else if (command && key === 'd') {
                event.preventDefault()
                pasteSnapshot(selectedSnapshot())
            } else if (!command && key === 'f') {
                event.preventDefault()
                reactFlowRef.current?.fitView({padding: 0.22, duration: 220})
            } else if (event.key === 'Escape') {
                clearSelection()
                setPendingConnection(null)
            }
        }
        window.addEventListener('keydown', handleKeyDown)
        return () => window.removeEventListener('keydown', handleKeyDown)
    }, [busy, canEdit, clearSelection, pasteSnapshot, redo, saveDraft, selectAll, selectedSnapshot, undo])

    const switchFlow = useCallback(async (id) => {
        if (!id || id === flowRef.current.id) return
        if (dirty && !window.confirm('当前流程有未保存改动，确定切换吗？')) return
        await runAction('flow-load', async () => {
            const document = await flowApi.flow(id)
            loadDocument(document, manifestIndexRef.current)
        })
    }, [dirty, loadDocument, runAction])

    const newFlow = useCallback(() => {
        if (dirty && !window.confirm('当前流程有未保存改动，确定新建吗？')) return
        loadDocument(createInitialFlow(), manifestIndexRef.current)
        openLibrary()
    }, [dirty, loadDocument, openLibrary])

    const handleCanvasReady = useCallback((instance) => {
        reactFlowRef.current = instance
        if (viewportRef.current) instance.setViewport(viewportRef.current)
    }, [])

    const handleMoveEnd = useCallback((_event, viewport) => {
        viewportRef.current = viewport
    }, [])

    const fitCanvas = useCallback(() => {
        reactFlowRef.current?.fitView({padding: 0.22, duration: 220})
    }, [])

    const closeLibrary = useCallback(() => {
        setPendingConnection(null)
        setLibraryOpen(false)
    }, [])

    const closeInspector = useCallback(() => {
        setInspectorOpen(false)
    }, [])

    const dockFlowSnapshot = useMemo(
        () => currentDocument(),
        [currentDocument, flow, inspectorValue]
    )

    const canUndo = historyRef.current.past.length > 0
    const canRedo = historyRef.current.future.length > 0
    void historyVersion

  return (
      <section className={`flow-editor-page${dockOpen ? ' dock-open' : ''}`}>
          <header className="flow-editor-header">
              <div className="flow-editor-document-switcher">
                  <select
                      value={flow.id || '__new__'}
                      onChange={(event) => switchFlow(event.target.value)}
                      aria-label="选择流程"
                  >
                      {!flow.id && <option value="__new__">新建流程</option>}
                      {flows.map((item) => <option key={item.id} value={item.id}>{item.name || item.code}</option>)}
                  </select>
                  <button type="button" onClick={newFlow} disabled={!canEdit} title="新建流程" aria-label="新建流程">
                      <FilePlus2 size={17} aria-hidden="true"/>
                  </button>
          </div>
              <FlowEditorToolbar
                  flowName={flow.name || ''}
                  status={flow.status || 'DRAFT'}
                  dirty={dirty}
                  busy={busy}
                  canEdit={canEdit}
                  canRun={canEdit && payloadIsValid}
                  canPublish={canPublish && (Boolean(flow.id) || canEdit)}
                  canUndo={canUndo}
                  canRedo={canRedo}
                  onNameChange={updateFlowName}
                  onUndo={undo}
                  onRedo={redo}
                  onSave={saveDraft}
                  onCompile={compileFlow}
                  onRun={debugFlow}
                  onPublish={publishFlow}
                  onFitView={fitCanvas}
              />
        </header>

          <div className="flow-editor-workspace">
              {libraryOpen ? (
                  <aside className="flow-editor-library-shell">
                      <NodeLibrary
                          components={libraryComponents}
                          disabled={!canEdit}
                          onAddNode={addNode}
                          onDragStart={handleLibraryDragStart}
                          onClose={closeLibrary}
                      />
                  </aside>
              ) : (
                  <button
                      type="button"
                      className="flow-editor-side-toggle library-toggle"
                      onClick={openLibrary}
                      title="打开节点库"
                      aria-label="打开节点库"
                  >
                      <PanelLeftOpen size={18} aria-hidden="true"/>
                  </button>
              )}

              <main className="flow-editor-graph-shell">
                  <FlowCanvas
                      nodes={displayNodes}
                      edges={canvasEdges}
                      canEdit={canEdit}
                      onConnect={handleConnect}
                      onReconnect={handleReconnect}
                      onSelectionChange={handleSelectionChange}
                      onNodeClick={handleCanvasNodeClick}
                      onPaneClick={clearSelection}
                      onDelete={handleDelete}
                      onNodeDragStop={handleNodeDragStop}
                      onAddAtPosition={addNode}
                      onOpenLibrary={openLibrary}
                      onMoveEnd={handleMoveEnd}
                      onReady={handleCanvasReady}
                      isValidConnection={connectionIsValid}
                  />
                  {!canEdit && <div className="flow-editor-readonly">只读模式</div>}
          </main>

              {selectedNode && inspectorOpen ? (
                  <aside className="flow-editor-inspector-shell">
                      <NodeInspector
                          node={!selectedNode.version && selectedManifest
                              ? {...selectedNode, version: selectedManifest.version}
                              : selectedNode}
                          manifest={selectedManifest}
                          value={inspectorValue}
                          onChange={handleInspectorValueChange}
                          onApply={applyNodeConfig}
                          onClose={closeInspector}
                          readOnly={!canEdit}
                      />
                  </aside>
              ) : selectedNode ? (
                  <button
                      type="button"
                      className="flow-editor-side-toggle inspector-toggle"
                      onClick={openInspector}
                      title="打开节点属性"
                      aria-label="打开节点属性"
                  >
                      <SlidersHorizontal size={18} aria-hidden="true"/>
                  </button>
              ) : null}
          </div>

          <FlowExecutionDock
              open={dockOpen}
              onOpenChange={handleDockOpenChange}
              payloadText={payloadText}
              onPayloadChange={setPayloadText}
              debugResult={debugResult}
              compileReport={compileReport}
              flow={dockFlowSnapshot}
              onRun={debugFlow}
              busy={busy}
              readOnly={!canEdit}
              selectedNodeId={selectedNodeId}
              selectedEdgeIds={selectedEdgeIds}
          />
      </section>
  )
}
