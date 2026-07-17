import {describe, expect, test} from 'vitest'
import {findMenuByAppPath, normalizeAppPath} from './navigationPolicy'

const routeTree = [
    {
        id: 'data-automation',
        type: 'DIR',
        children: [
            {id: 'flow', type: 'MENU', name: '流程设计器'},
            {id: 'flow-run-detail', type: 'MENU', name: '运行详情', path: '/flow/runs/:runId'}
        ]
    }
]

describe('admin navigation path matching', () => {
    test('normalizes app paths without query strings or fragments', () => {
        expect(normalizeAppPath('/admin/flow?flowId=flow-1#node-a')).toBe('/admin/flow')
    })

    test('matches direct and aliased routes that include query strings', () => {
        expect(findMenuByAppPath(routeTree, '/admin/flow?flowId=flow-1')?.id).toBe('flow')
        expect(findMenuByAppPath(routeTree, '/admin/flows/studio?flowId=flow-1#node-a')?.id).toBe('flow')
    })

    test('matches dynamic routes that include query strings', () => {
        expect(findMenuByAppPath(routeTree, '/admin/flow/runs/run-1?tab=events')?.id).toBe('flow-run-detail')
    })
})
