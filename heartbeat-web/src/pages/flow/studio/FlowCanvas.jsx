import {memo, useCallback, useEffect, useRef, useState} from 'react'
import {
    Background,
    BackgroundVariant,
    ConnectionLineType,
    Controls,
    MarkerType,
    MiniMap,
    Panel,
    ReactFlow,
    ReactFlowProvider,
    SelectionMode
} from '@xyflow/react'
import {Plus} from 'lucide-react'
import FlowNodeCard from '../../../components/flow/FlowNodeCard'

const nodeTypes = {flowNode: FlowNodeCard}
const defaultEdgeOptions = {
    type: 'smoothstep',
    markerEnd: {type: MarkerType.ArrowClosed, width: 16, height: 16},
    style: {strokeWidth: 1.6}
}

function coarsePointerMatches() {
    return typeof window !== 'undefined' && window.matchMedia?.('(pointer: coarse)').matches
}

function FlowCanvasInner({
                             nodes: externalNodes,
                             edges: externalEdges,
                             canEdit,
                             onConnect,
                             onReconnect,
                             onSelectionChange,
                             onNodeClick,
                             onPaneClick,
                             onDelete,
                             onNodeDragStop,
                             onAddAtPosition,
                             onOpenLibrary,
                             onMoveEnd,
                             onReady,
                             isValidConnection
                         }) {
    const instanceRef = useRef(null)
    const [coarsePointer, setCoarsePointer] = useState(coarsePointerMatches)
    const [nodeDragging, setNodeDragging] = useState(false)

    useEffect(() => {
        instanceRef.current?.setNodes(externalNodes)
    }, [externalNodes])

    useEffect(() => {
        instanceRef.current?.setEdges(externalEdges)
    }, [externalEdges])

    useEffect(() => {
        const media = window.matchMedia?.('(pointer: coarse)')
        if (!media) return undefined
        const handleChange = () => setCoarsePointer(media.matches)
        handleChange()
        media.addEventListener?.('change', handleChange)
        return () => media.removeEventListener?.('change', handleChange)
    }, [])

    const handleInit = useCallback((instance) => {
        instanceRef.current = instance
        onReady?.(instance)
    }, [onReady])

    const handleDragOver = useCallback((event) => {
        event.preventDefault()
        event.dataTransfer.dropEffect = canEdit ? 'copy' : 'none'
    }, [canEdit])

    const handleDrop = useCallback((event) => {
        event.preventDefault()
        if (!canEdit || !instanceRef.current) return
        const raw = event.dataTransfer.getData('application/heartbeat-flow-component')
            || event.dataTransfer.getData('application/reactflow')
        if (!raw) return
        let componentRef = raw
        try {
            const parsed = JSON.parse(raw)
            componentRef = parsed?.type ? parsed : raw
        } catch {
            componentRef = raw
        }
        const position = instanceRef.current.screenToFlowPosition({
            x: event.clientX,
            y: event.clientY
        }, {snapToGrid: true})
        onAddAtPosition?.(componentRef, position)
    }, [canEdit, onAddAtPosition])

    const handleNodeDragStart = useCallback(() => {
        setNodeDragging(true)
    }, [])

    const handleNodeDragStop = useCallback((event, node, draggedNodes) => {
        setNodeDragging(false)
        onNodeDragStop?.(event, node, draggedNodes)
    }, [onNodeDragStop])

    return (
        <div
            className={`flow-editor-canvas${nodeDragging ? ' is-dragging' : ''}`}
            onDragOver={handleDragOver}
            onDrop={handleDrop}
        >
            <ReactFlow
                defaultNodes={externalNodes}
                defaultEdges={externalEdges}
                nodeTypes={nodeTypes}
                defaultEdgeOptions={defaultEdgeOptions}
                connectionLineType={ConnectionLineType.SmoothStep}
                connectionLineStyle={{strokeWidth: 2}}
                onInit={handleInit}
                onConnect={onConnect}
                onReconnect={onReconnect}
                onSelectionChange={onSelectionChange}
                onNodeClick={onNodeClick}
                onPaneClick={onPaneClick}
                onDelete={onDelete}
                onNodeDragStart={handleNodeDragStart}
                onNodeDragStop={handleNodeDragStop}
                onMoveEnd={onMoveEnd}
                isValidConnection={isValidConnection}
                nodesDraggable={canEdit}
                nodesConnectable={canEdit && !nodeDragging}
                edgesReconnectable={canEdit && !nodeDragging}
                elementsSelectable={!nodeDragging}
                deleteKeyCode={canEdit ? ['Backspace', 'Delete'] : null}
                multiSelectionKeyCode={['Meta', 'Control', 'Shift']}
                selectionKeyCode={coarsePointer ? null : 'Shift'}
                selectionMode={SelectionMode.Partial}
                selectionOnDrag={!coarsePointer}
                panOnDrag={coarsePointer ? true : [1, 2]}
                panOnScroll
                zoomOnScroll={false}
                zoomOnPinch
                zoomOnDoubleClick={false}
                minZoom={0.2}
                maxZoom={1.8}
                snapToGrid
                snapGrid={[16, 16]}
                fitView
                fitViewOptions={{padding: 0.22, maxZoom: 1.1}}
                onlyRenderVisibleElements
                elevateNodesOnSelect
                aria-label="Flow 工作流画布"
            >
                <Background variant={BackgroundVariant.Dots} gap={20} size={1.2}/>
                {!nodeDragging && externalNodes.length <= 80 && (
                    <MiniMap
                        className="flow-editor-minimap"
                        pannable
                        zoomable
                        nodeStrokeWidth={3}
                        nodeColor={(node) => node.data?.accent || '#64748b'}
                    />
                )}
                <Controls className="flow-editor-controls" showInteractive={false}/>
                <Panel position="top-left" className="flow-editor-canvas-panel">
                    <button
                        type="button"
                        className="flow-editor-add-button"
                        onClick={onOpenLibrary}
                        disabled={!canEdit}
                        title="添加节点"
                        aria-label="添加节点"
                    >
                        <Plus size={17} aria-hidden="true"/>
                        <span>添加节点</span>
                    </button>
                </Panel>
            </ReactFlow>
            {externalNodes.length === 0 && (
                <div className="flow-editor-empty">
                    <div className="flow-editor-empty-mark"><Plus size={22} aria-hidden="true"/></div>
                    <strong>画布为空</strong>
                    <span>0 个节点</span>
                    <button type="button" onClick={onOpenLibrary} disabled={!canEdit}>打开节点库</button>
                </div>
            )}
        </div>
    )
}

function FlowCanvas(props) {
    return (
        <ReactFlowProvider>
            <FlowCanvasInner {...props} />
        </ReactFlowProvider>
    )
}

export default memo(FlowCanvas)
