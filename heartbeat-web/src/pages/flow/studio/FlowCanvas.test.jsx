import {useState} from 'react'
import {fireEvent, render, screen} from '@testing-library/react'
import {expect, it, vi} from 'vitest'
import FlowCanvas from './FlowCanvas'

vi.mock('@xyflow/react', async () => {
    const actual = await vi.importActual('@xyflow/react')

    function ReactFlowMock({defaultNodes, onInit, onNodeDragStop, children}) {
        const [nodes, setNodes] = useState(defaultNodes)
        const [edges, setEdges] = useState([])
        const instance = {setNodes, setEdges}
        if (!ReactFlowMock.initialized) {
            ReactFlowMock.initialized = true
            queueMicrotask(() => onInit?.(instance))
        }
        return (
            <div>
                <output data-testid="node-position">{nodes[0]?.position?.x ?? -1}</output>
                <button
                    type="button"
                    onClick={() => setNodes((current) => current.map((node) => node.id === 'node-1'
                        ? {...node, position: {x: 96, y: 32}}
                        : node))}
                >move node
                </button>
                <button
                    type="button"
                    onClick={() => onNodeDragStop({}, nodes[0], [nodes[0]])}
                >stop dragging
                </button>
                <span data-testid="edge-count">{edges.length}</span>
                {children}
            </div>
        )
    }

    return {
        ...actual,
        ReactFlowProvider: ({children}) => children,
        ReactFlow: ReactFlowMock,
        Background: () => null,
        Controls: () => null,
        MiniMap: () => null,
        Panel: ({children}) => children
    }
})

it('keeps transient drag positions inside the canvas until drag stop', () => {
    const sourceNodes = [{
        id: 'node-1',
        type: 'flowNode',
        position: {x: 0, y: 0},
        data: {node: {id: 'node-1'}}
    }]
    const onNodeDragStop = vi.fn()

    render(
        <FlowCanvas
            nodes={sourceNodes}
            edges={[]}
            canEdit
            onNodeDragStop={onNodeDragStop}
        />
    )

    fireEvent.click(screen.getByRole('button', {name: 'move node'}))

    expect(screen.getByTestId('node-position')).toHaveTextContent('96')
    expect(sourceNodes[0].position.x).toBe(0)
    expect(onNodeDragStop).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', {name: 'stop dragging'}))

    expect(onNodeDragStop).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({id: 'node-1', position: {x: 96, y: 32}}),
        [expect.objectContaining({id: 'node-1', position: {x: 96, y: 32}})]
    )
})
