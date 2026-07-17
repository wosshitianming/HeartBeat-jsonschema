import {fireEvent, render, screen} from '@testing-library/react'
import {expect, it, vi} from 'vitest'
import FlowNodeCard from './FlowNodeCard'

vi.mock('@xyflow/react', () => ({
    Handle: ({id, type, isConnectable, 'aria-label': ariaLabel}) => (
        <button
            type="button"
            data-handle-id={id}
            data-handle-type={type}
            aria-label={ariaLabel}
            disabled={!isConnectable}
        />
    ),
    Position: {Left: 'left', Right: 'right'}
}))

const manifest = {
    type: 'logic.branch',
    version: '1.0.0',
    name: '条件分支',
    category: '逻辑',
    icon: 'condition',
    ports: {
        inputs: [
            {id: 'input', label: '输入', schema: 'object', required: true},
            {id: 'fallback', label: '备用输入', schema: 'object'}
        ],
        outputs: [
            {id: 'true', label: '满足条件', schema: 'object'},
            {id: 'false', label: '不满足条件', schema: 'object'}
        ]
    }
}

function renderNode(data = {}, props = {}) {
    return render(
        <FlowNodeCard
            data={{
                node: {
                    id: 'branch-1',
                    type: manifest.type,
                    version: manifest.version,
                    config: {expression: '$json.enabled', internalValue: '不应显示在画布'},
                    ...data.node
                },
                manifest,
                component: manifest,
                accent: '#c24167',
                ...data
            }}
            {...props}
        />
    )
}

it('renders a compact identity without embedding configuration details', () => {
    renderNode()

    expect(screen.getByText('条件分支')).toBeInTheDocument()
    expect(screen.getByText('逻辑')).toBeInTheDocument()
    expect(screen.queryByText('$json.enabled')).not.toBeInTheDocument()
    expect(screen.queryByText('不应显示在画布')).not.toBeInTheDocument()
    expect(document.querySelector('.flow-editor-node-config-summary')).not.toBeInTheDocument()
    expect(document.querySelector('.flow-editor-node-footer')).not.toBeInTheDocument()
})

it('preserves every input and output handle and quick-add action', () => {
    const onQuickAdd = vi.fn()
    renderNode({onQuickAdd})

    expect(screen.getAllByRole('button', {name: /输入端口/})).toHaveLength(2)
    expect(screen.getAllByRole('button', {name: /输出端口/})).toHaveLength(2)

    fireEvent.click(screen.getByRole('button', {name: '从“满足条件”添加后续节点'}))

    expect(onQuickAdd).toHaveBeenCalledWith('branch-1', 'true')
})

it('keeps execution status and elapsed time accessible', () => {
    renderNode({executionState: {status: 'failed', elapsedMs: 37}})

    expect(screen.getByLabelText('执行失败，耗时 37 ms')).toBeInTheDocument()
    expect(document.querySelector('.flow-editor-node-card')).toHaveAttribute('data-execution', 'failed')
})
