import {describe, expect, it} from 'vitest'
import {
    buildManifestIndex,
    canvasToFlowDsl,
    decorateCanvasNodes,
    flowDslToCanvas,
    resolveManifest,
    validateConnection
} from './flowCanvasAdapter'

const manifests = [
    {
        type: 'trigger.manual',
        version: '1.0.0',
        name: '手动触发',
        category: '触发器',
        ports: {inputs: [], outputs: [{id: 'out', label: '输出'}]}
    },
    {
        type: 'action.task',
        version: '1.0.0',
        name: '任务 v1',
        category: '动作',
        ports: {inputs: [{id: 'in', label: '输入'}], outputs: [{id: 'out', label: '输出'}]}
    },
    {
        type: 'action.task',
        version: '2.0.0',
        name: '任务 v2',
        category: '动作',
        ports: {inputs: [{id: 'payload', label: '载荷'}], outputs: [{id: 'next', label: '后续'}]}
    }
]

function node(id, type = 'action.task', version = '1.0.0') {
    return {
        id,
        type: 'flowNode',
        position: {x: 0, y: 0},
        data: {
            node: {id, type, version, config: {}},
            manifest: manifests.find((item) => item.type === type && item.version === version)
        }
    }
}

describe('flowCanvasAdapter', () => {
    it('resolves exact versions and uses backend-compatible 1.0.0 for an empty version', () => {
        const index = buildManifestIndex(manifests)

        expect(resolveManifest(index, 'action.task', '2.0.0')?.name).toBe('任务 v2')
        expect(resolveManifest(index, 'action.task')?.name).toBe('任务 v1')
        expect(resolveManifest(index, 'action.task', '3.0.0')).toBeNull()
    })

    it('round-trips ports and extension metadata without leaking React Flow fields', () => {
        const flow = {
            id: 'flow-1',
            name: 'Round trip',
            variables: {},
            settings: {editor: {custom: true}},
            nodes: [
                {
                    id: 'start',
                    type: 'trigger.manual',
                    version: '1.0.0',
                    position: {x: 12, y: 18},
                    config: {},
                    extension: 'keep-node'
                },
                {id: 'task', type: 'action.task', version: '2.0.0', position: {x: 302, y: 18}, config: {mode: 'fast'}}
            ],
            edges: [
                {
                    id: 'edge-1',
                    source: 'start',
                    sourcePort: 'out',
                    target: 'task',
                    targetPort: 'payload',
                    extension: 'keep-edge'
                }
            ]
        }
        const canvas = flowDslToCanvas(flow, buildManifestIndex(manifests))
        const roundTrip = canvasToFlowDsl(flow, canvas.nodes, canvas.edges)

        expect(canvas.nodes[1].type).toBe('flowNode')
        expect(canvas.edges[0]).toMatchObject({sourceHandle: 'out', targetHandle: 'payload'})
        expect(roundTrip.nodes[0]).toMatchObject({extension: 'keep-node', position: {x: 12, y: 18}})
        expect(roundTrip.edges[0]).toMatchObject({extension: 'keep-edge', sourcePort: 'out', targetPort: 'payload'})
        expect(roundTrip.nodes[0]).not.toHaveProperty('data')
        expect(roundTrip.edges[0]).not.toHaveProperty('sourceHandle')
    })

    it('quarantines orphan edges so the editor can repair a stale document', () => {
        const flow = {
            nodes: [{id: 'task', type: 'action.task', version: '1.0.0', position: {x: 0, y: 0}, config: {}}],
            edges: [{id: 'orphan', source: 'missing', target: 'task'}]
        }
        const canvas = flowDslToCanvas(flow, buildManifestIndex(manifests))
        const roundTrip = canvasToFlowDsl(flow, canvas.nodes, canvas.edges)

        expect(canvas.edges[0].hidden).toBe(true)
        expect(roundTrip.edges).toEqual([])

        const index = buildManifestIndex(manifests)
        const editorNodes = [node('start', 'trigger.manual'), node('task')]
        expect(validateConnection(
            {source: 'start', sourceHandle: 'out', target: 'task', targetHandle: 'in'},
            editorNodes,
            canvas.edges,
            index
        )).toEqual({valid: true, reason: ''})
    })

    it('reuses unchanged presentation nodes while a different node is being dragged', () => {
        const index = buildManifestIndex(manifests)
        const sourceNodes = [node('a'), node('b')]
        const onQuickAdd = () => {
        }
        const first = decorateCanvasNodes(sourceNodes, {
            manifestIndex: index,
            debugByNode: new Map(),
            canEdit: true,
            onQuickAdd
        })
        const movedNodes = [
            {...sourceNodes[0], position: {x: 16, y: 0}},
            sourceNodes[1]
        ]
        const second = decorateCanvasNodes(movedNodes, {
            manifestIndex: index,
            debugByNode: new Map(),
            canEdit: true,
            onQuickAdd
        }, first.cache)

        expect(second.nodes[0]).not.toBe(first.nodes[0])
        expect(second.nodes[1]).toBe(first.nodes[1])
        expect(second.nodes[1].data).toMatchObject({readOnly: false, onQuickAdd})
    })

    it('rejects occupied inputs, duplicates, self-links, and directed cycles', () => {
        const index = buildManifestIndex(manifests)
        const nodes = [node('a', 'trigger.manual'), node('b'), node('c')]
        const edges = [
            {id: 'ab', source: 'a', sourceHandle: 'out', target: 'b', targetHandle: 'in'},
            {id: 'bc', source: 'b', sourceHandle: 'out', target: 'c', targetHandle: 'in'}
        ]

        expect(validateConnection({
            source: 'a',
            sourceHandle: 'out',
            target: 'a',
            targetHandle: 'in'
        }, nodes, [], index).valid).toBe(false)
        expect(validateConnection({
            source: 'a',
            sourceHandle: 'out',
            target: 'b',
            targetHandle: 'in'
        }, nodes, edges, index).valid).toBe(false)
        expect(validateConnection({
            source: 'c',
            sourceHandle: 'out',
            target: 'a',
            targetHandle: 'out'
        }, nodes, edges, index).valid).toBe(false)

        const cycleNodes = [node('x'), node('y'), node('z')]
        const cycleEdges = [
            {id: 'xy', source: 'x', sourceHandle: 'out', target: 'y', targetHandle: 'in'},
            {id: 'yz', source: 'y', sourceHandle: 'out', target: 'z', targetHandle: 'in'}
        ]
        expect(validateConnection(
            {source: 'z', sourceHandle: 'out', target: 'x', targetHandle: 'in'},
            cycleNodes,
            cycleEdges,
            index
        )).toMatchObject({valid: false, reason: '当前流程不支持循环连接'})
    })
})
