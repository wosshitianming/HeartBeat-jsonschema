const CATEGORY_COLORS = {
    '触发器': '#0f8a7a',
    '数据源': '#2563eb',
    '消息': '#7c3aed',
    '动作': '#db5b20',
    '逻辑': '#c24167',
    '转换': '#6d5f18',
    '输出': '#16803c',
    '系统': '#596579'
}

let fallbackIdSequence = 0

function clone(value, fallback) {
    if (value == null) return fallback
    return JSON.parse(JSON.stringify(value))
}

function manifestKey(type, version) {
    return `${type || ''}@${version || ''}`
}

function naturalVersionCompare(left, right) {
    return String(right?.version || '').localeCompare(String(left?.version || ''), undefined, {
        numeric: true,
        sensitivity: 'base'
    })
}

function finiteCoordinate(value, fallback = 0) {
    const number = Number(value)
    return Number.isFinite(number) ? number : fallback
}

function domainNodeFromCanvas(node) {
    return node?.data?.node || node?.data?.domainNode || null
}

function manifestForCanvasNode(node, index) {
    const domainNode = domainNodeFromCanvas(node)
    return resolveManifest(index, domainNode?.type, domainNode?.version)
}

function portById(ports, id) {
    return (ports || []).find((port) => port?.id === id) || null
}

function resolvedHandle(edge, direction) {
    const handleKey = direction === 'source' ? 'sourceHandle' : 'targetHandle'
    const domainKey = direction === 'source' ? 'sourcePort' : 'targetPort'
    const stored = edge?.[handleKey] ?? edge?.data?.domainEdge?.[domainKey]
    return stored ?? null
}

function createsDirectedCycle(source, target, edges, replacingEdgeId) {
    const adjacency = new Map()
    for (const edge of edges || []) {
        if (!edge?.source || !edge?.target || edge.id === replacingEdgeId) continue
        if (!adjacency.has(edge.source)) adjacency.set(edge.source, new Set())
        adjacency.get(edge.source).add(edge.target)
    }
    if (!adjacency.has(source)) adjacency.set(source, new Set())
    adjacency.get(source).add(target)

    const pending = [target]
    const visited = new Set()
    while (pending.length) {
        const current = pending.pop()
        if (current === source) return true
        if (visited.has(current)) continue
        visited.add(current)
        for (const next of adjacency.get(current) || []) pending.push(next)
    }
    return false
}

export function createStableId(prefix = 'item') {
    const safePrefix = String(prefix || 'item')
        .replace(/[^a-zA-Z0-9_-]+/g, '_')
        .replace(/^_+|_+$/g, '')
        .toLowerCase() || 'item'
    const randomId = globalThis.crypto?.randomUUID?.()
    if (randomId) return `${safePrefix}_${randomId.replaceAll('-', '')}`
    fallbackIdSequence += 1
    return `${safePrefix}_${Date.now().toString(36)}_${fallbackIdSequence.toString(36)}`
}

export function buildManifestIndex(manifests = []) {
    const byKey = new Map()
    const byType = new Map()
    for (const manifest of manifests || []) {
        if (!manifest?.type || !manifest?.version) continue
        byKey.set(manifestKey(manifest.type, manifest.version), manifest)
        if (!byType.has(manifest.type)) byType.set(manifest.type, [])
        byType.get(manifest.type).push(manifest)
    }
    for (const versions of byType.values()) versions.sort(naturalVersionCompare)
    return {byKey, byType}
}

export function resolveManifest(index, type, version) {
    if (!index || !type) return null
    if (version) return index.byKey?.get(manifestKey(type, version)) || null
    return index.byKey?.get(manifestKey(type, '1.0.0')) || index.byType?.get(type)?.[0] || null
}

export function flowDslToCanvas(flow = {}, manifestIndex = buildManifestIndex()) {
    const domainNodes = Array.isArray(flow?.nodes) ? flow.nodes : []
    const nodes = domainNodes.map((domainNode, index) => {
        const id = domainNode?.id || createStableId('node')
        const manifest = resolveManifest(manifestIndex, domainNode?.type, domainNode?.version)
        const position = domainNode?.position || {}
        return {
            id,
            type: 'flowNode',
            position: {
                x: finiteCoordinate(position.x, 112 + (index % 4) * 256),
                y: finiteCoordinate(position.y, 96 + Math.floor(index / 4) * 128)
            },
            data: {
                node: {
                    ...clone(domainNode, {}),
                    id,
                    type: domainNode?.type || '',
                    version: domainNode?.version || manifest?.version || '1.0.0',
                    position: clone(domainNode?.position, {x: 0, y: 0}),
                    config: clone(domainNode?.config, {})
                },
                manifest,
                component: manifest,
                accent: CATEGORY_COLORS[manifest?.category] || '#596579'
            }
        }
    })

    const canvasNodeById = new Map(nodes.map((node) => [node.id, node]))
    const edges = (Array.isArray(flow?.edges) ? flow.edges : [])
        .filter((edge) => edge?.source && edge?.target)
        .map((domainEdge) => {
            const sourceHandle = domainEdge.sourcePort ?? null
            const targetHandle = domainEdge.targetPort ?? null
            const hidden = !canvasNodeById.has(domainEdge.source) || !canvasNodeById.has(domainEdge.target)
            return {
                id: domainEdge.id || createStableId('edge'),
                source: domainEdge.source,
                sourceHandle,
                target: domainEdge.target,
                targetHandle,
                type: 'smoothstep',
                hidden,
                data: {
                    domainEdge: {
                        ...clone(domainEdge, {}),
                        id: domainEdge.id,
                        source: domainEdge.source,
                        sourcePort: sourceHandle,
                        target: domainEdge.target,
                        targetPort: targetHandle
                    }
                }
            }
        })

    return {nodes, edges}
}

export function decorateCanvasNodes(canvasNodes = [], {
    manifestIndex = buildManifestIndex(),
    debugByNode = new Map(),
    canEdit = false,
    onQuickAdd
} = {}, previousCache = new Map()) {
    const nextCache = new Map()
    const nodes = (canvasNodes || []).map((canvasNode) => {
        const domainNode = domainNodeFromCanvas(canvasNode) || {}
        const manifest = resolveManifest(manifestIndex, domainNode.type, domainNode.version)
        const execution = debugByNode.get(canvasNode.id)
        const cached = previousCache.get(canvasNode.id)
        if (cached
            && cached.source === canvasNode
            && cached.domainNode === domainNode
            && cached.manifest === manifest
            && cached.execution === execution
            && cached.canEdit === canEdit
            && cached.onQuickAdd === onQuickAdd) {
            nextCache.set(canvasNode.id, cached)
            return cached.output
        }

        const output = {
            ...canvasNode,
            data: {
                ...canvasNode.data,
                node: domainNode,
                manifest,
                component: manifest,
                debugStatus: execution,
                executionState: execution,
                readOnly: !canEdit,
                onQuickAdd
            }
        }
        nextCache.set(canvasNode.id, {
            source: canvasNode,
            domainNode,
            manifest,
            execution,
            canEdit,
            onQuickAdd,
            output
        })
        return output
    })
    return {nodes, cache: nextCache}
}

export function canvasToFlowDsl(flow = {}, canvasNodes = [], canvasEdges = []) {
    const canvasNodeIds = new Set((canvasNodes || []).map((node) => node.id))
    const nodes = (canvasNodes || []).map((canvasNode) => {
        const domainNode = domainNodeFromCanvas(canvasNode) || {}
        return {
            ...clone(domainNode, {}),
            id: canvasNode.id,
            type: domainNode.type || '',
            version: domainNode.version || '',
            position: {
                x: Math.round(finiteCoordinate(canvasNode.position?.x)),
                y: Math.round(finiteCoordinate(canvasNode.position?.y))
            },
            config: clone(domainNode.config, {})
        }
    })
    const edges = (canvasEdges || [])
        .filter((edge) => edge?.source && edge?.target
            && canvasNodeIds.has(edge.source) && canvasNodeIds.has(edge.target))
        .map((canvasEdge) => {
            const domainEdge = canvasEdge?.data?.domainEdge || {}
            return {
                ...clone(domainEdge, {}),
                id: canvasEdge.id || createStableId('edge'),
                source: canvasEdge.source,
                sourcePort: resolvedHandle(canvasEdge, 'source'),
                target: canvasEdge.target,
                targetPort: resolvedHandle(canvasEdge, 'target')
            }
        })

    return {
        ...clone(flow, {}),
        variables: clone(flow?.variables, {}),
        nodes,
        edges,
        settings: clone(flow?.settings, {})
    }
}

export function validateConnection(connection, nodes = [], edges = [], manifestIndex = buildManifestIndex()) {
    const source = connection?.source
    const target = connection?.target
    if (!source || !target) return {valid: false, reason: '连接必须同时包含源节点和目标节点'}
    if (source === target) return {valid: false, reason: '节点不能连接到自身'}

    const nodeById = new Map((nodes || []).map((node) => [node.id, node]))
    const sourceNode = nodeById.get(source)
    const targetNode = nodeById.get(target)
    if (!sourceNode || !targetNode) return {valid: false, reason: '连接节点不存在'}

    const sourceManifest = manifestForCanvasNode(sourceNode, manifestIndex)
    const targetManifest = manifestForCanvasNode(targetNode, manifestIndex)
    if (!sourceManifest || !targetManifest) return {valid: false, reason: '节点组件版本未注册或未启用'}

    const sourceHandle = connection.sourceHandle || sourceManifest.ports?.outputs?.[0]?.id
    const targetHandle = connection.targetHandle || targetManifest.ports?.inputs?.[0]?.id
    if (!portById(sourceManifest.ports?.outputs, sourceHandle)) {
        return {valid: false, reason: '源节点输出端口不存在'}
    }
    if (!portById(targetManifest.ports?.inputs, targetHandle)) {
        return {valid: false, reason: '目标节点输入端口不存在'}
    }

    const replacingEdgeId = connection.replacingEdgeId
    const activeEdges = (edges || []).filter((edge) => edge.id !== replacingEdgeId
        && !edge.hidden
        && nodeById.has(edge.source)
        && nodeById.has(edge.target))
    const duplicate = activeEdges.some((edge) => edge.source === source
        && (edge.sourceHandle || null) === (sourceHandle || null)
        && edge.target === target
        && (edge.targetHandle || null) === (targetHandle || null))
    if (duplicate) return {valid: false, reason: '相同端口之间已经存在连接'}

    const occupied = activeEdges.some((edge) => edge.target === target
        && (edge.targetHandle || null) === (targetHandle || null))
    if (occupied) return {valid: false, reason: '该输入端口已经连接'}

    if (createsDirectedCycle(source, target, activeEdges, replacingEdgeId)) {
        return {valid: false, reason: '当前流程不支持循环连接'}
    }
    return {valid: true, reason: ''}
}
